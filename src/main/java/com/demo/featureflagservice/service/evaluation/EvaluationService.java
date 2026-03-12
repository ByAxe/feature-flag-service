package com.demo.featureflagservice.service.evaluation;

import com.demo.featureflagservice.domain.EvaluationLog;
import com.demo.featureflagservice.domain.FeatureFlag;
import com.demo.featureflagservice.logging.EvaluationAuditLogger;
import com.demo.featureflagservice.repository.EvaluationLogRepository;
import com.demo.featureflagservice.service.FeatureFlagService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EvaluationService {

    private final FeatureFlagService featureFlagService;
    private final HashingRolloutStrategy rolloutStrategy;
    private final EvaluationLogRepository evaluationLogRepository;
    private final EvaluationAuditLogger auditLogger;

    public EvaluationService(
            FeatureFlagService featureFlagService,
            HashingRolloutStrategy rolloutStrategy,
            EvaluationLogRepository evaluationLogRepository,
            EvaluationAuditLogger auditLogger) {
        this.featureFlagService = featureFlagService;
        this.rolloutStrategy = rolloutStrategy;
        this.evaluationLogRepository = evaluationLogRepository;
        this.auditLogger = auditLogger;
    }

    public EvaluationResult evaluate(String flagKey, String userId) {
        return evaluate(flagKey, userId, null);
    }

    public EvaluationResult evaluate(String flagKey, String userId, String requestId) {
        long startedAt = System.nanoTime();
        String resolvedRequestId = resolveRequestId(requestId);
        String resolvedFlagKey = flagKey;
        String resolvedUserId = userId;
        try {
            resolvedFlagKey = canonicalizeFlagKey(flagKey);
            resolvedUserId = normalizeUserId(userId);
            FeatureFlag featureFlag = featureFlagService.getByKey(resolvedFlagKey);
            EvaluationResult result = evaluate(featureFlag, resolvedUserId);
            persistLog(result, resolvedUserId);
            auditLogger.logSuccess(resolvedRequestId, resolvedUserId, result, elapsedMillis(startedAt));
            return result;
        } catch (RuntimeException exception) {
            auditLogger.logFailure(resolvedRequestId, resolvedFlagKey, resolvedUserId, exception.getMessage(), elapsedMillis(startedAt));
            throw exception;
        }
    }

    public List<EvaluationResult> evaluateAll(String userId) {
        return evaluateAll(userId, null);
    }

    public List<EvaluationResult> evaluateAll(String userId, String requestId) {
        String resolvedRequestId = resolveRequestId(requestId);
        String normalizedUserId = normalizeUserId(userId);
        return featureFlagService.list().stream()
                .map(featureFlag -> evaluate(featureFlag.key(), normalizedUserId, resolvedRequestId))
                .toList();
    }

    EvaluationResult evaluate(FeatureFlag featureFlag, String userId) {
        if (featureFlag == null) {
            throw new IllegalArgumentException("flag must not be null");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (!featureFlag.isEnabled()) {
            return new EvaluationResult(featureFlag.getKey(), false, EvaluationReason.DISABLED);
        }
        if (featureFlag.getTargetUserIds().contains(userId)) {
            return new EvaluationResult(featureFlag.getKey(), true, EvaluationReason.TARGET_LIST);
        }
        if (featureFlag.getRolloutPercentage() == 100) {
            return new EvaluationResult(featureFlag.getKey(), true, EvaluationReason.FULL_ROLLOUT);
        }
        if (featureFlag.getRolloutPercentage() == 0) {
            return new EvaluationResult(featureFlag.getKey(), false, EvaluationReason.NO_ROLLOUT);
        }
        boolean enabled = rolloutStrategy.isEnabled(featureFlag.getKey(), userId, featureFlag.getRolloutPercentage());
        return new EvaluationResult(featureFlag.getKey(), enabled, EvaluationReason.PERCENTAGE_ROLLOUT);
    }

    private void persistLog(EvaluationResult result, String userId) {
        EvaluationLog log = new EvaluationLog();
        log.setFlagKey(result.flagKey());
        log.setUserId(userId);
        log.setResult(result.enabled());
        log.setReason(result.reason().name());
        log.setEvaluatedAt(Instant.now());
        evaluationLogRepository.save(log);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String canonicalizeFlagKey(String flagKey) {
        if (flagKey == null || flagKey.isBlank()) {
            throw new IllegalArgumentException("flagKey is required");
        }
        return flagKey.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        return userId.trim();
    }

    private String resolveRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
    }
}

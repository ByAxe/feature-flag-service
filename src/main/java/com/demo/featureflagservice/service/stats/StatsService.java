package com.demo.featureflagservice.service.stats;

import com.demo.featureflagservice.dto.FlagStatsResponse;
import com.demo.featureflagservice.dto.GlobalStatsResponse;
import com.demo.featureflagservice.domain.FeatureFlag;
import com.demo.featureflagservice.repository.EvaluationLogRepository;
import com.demo.featureflagservice.service.FeatureFlagService;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final EvaluationLogRepository evaluationLogRepository;
    private final FeatureFlagService featureFlagService;

    public StatsService(EvaluationLogRepository evaluationLogRepository, FeatureFlagService featureFlagService) {
        this.evaluationLogRepository = evaluationLogRepository;
        this.featureFlagService = featureFlagService;
    }

    public GlobalStatsResponse getGlobalStats() {
        Object[] stats = evaluationLogRepository.fetchGlobalStats();
        long totalEvaluations = stats[0] == null ? 0L : ((Number) stats[0]).longValue();
        long uniqueUsers = stats[1] == null ? 0L : ((Number) stats[1]).longValue();
        long activeFlagsCount = featureFlagService.countActiveFlags();
        return new GlobalStatsResponse(totalEvaluations, uniqueUsers, activeFlagsCount);
    }

    public FlagStatsResponse getFlagStats(String flagKey) {
        FeatureFlag featureFlag = featureFlagService.getByKey(flagKey);
        Object[] stats = evaluationLogRepository.fetchFlagStats(featureFlag.getKey());
        long totalEvaluations = asLong(stats[0]);
        long uniqueUsers = asLong(stats[1]);
        long trueCount = asLong(stats[2]);
        long falseCount = asLong(stats[3]);
        Instant lastEvaluatedAt = (Instant) stats[4];
        double trueRatio = totalEvaluations == 0 ? 0.0d : (double) trueCount / totalEvaluations;
        double falseRatio = totalEvaluations == 0 ? 0.0d : (double) falseCount / totalEvaluations;
        return new FlagStatsResponse(
                featureFlag.getKey(),
                totalEvaluations,
                uniqueUsers,
                trueCount,
                falseCount,
                trueRatio,
                falseRatio,
                lastEvaluatedAt);
    }

    private long asLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}

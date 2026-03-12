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
        var stats = evaluationLogRepository.fetchGlobalStats();
        long totalEvaluations = stats.totalEvaluations();
        long uniqueUsers = stats.uniqueUsers();
        long activeFlagsCount = featureFlagService.countActiveFlags();
        return new GlobalStatsResponse(totalEvaluations, uniqueUsers, activeFlagsCount);
    }

    public FlagStatsResponse getFlagStats(String flagKey) {
        FeatureFlag featureFlag = featureFlagService.getByKey(flagKey);
        var stats = evaluationLogRepository.fetchFlagStats(featureFlag.getKey());
        long totalEvaluations = stats.totalEvaluations();
        long uniqueUsers = stats.uniqueUsers();
        long trueCount = stats.trueCount() == null ? 0L : stats.trueCount();
        long falseCount = stats.falseCount() == null ? 0L : stats.falseCount();
        Instant lastEvaluatedAt = stats.lastEvaluatedAt();
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
}

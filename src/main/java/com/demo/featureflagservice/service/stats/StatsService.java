package com.demo.featureflagservice.service.stats;

import com.demo.featureflagservice.dto.FlagStatsResponse;
import com.demo.featureflagservice.dto.GlobalStatsResponse;
import com.demo.featureflagservice.domain.FeatureFlag;
import com.demo.featureflagservice.repository.EvaluationLogRepository;
import com.demo.featureflagservice.service.FeatureFlagService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
        List<Object> stats = flatten(evaluationLogRepository.fetchGlobalStats());
        long totalEvaluations = stats.size() > 0 ? asLong(stats.get(0)) : 0L;
        long uniqueUsers = stats.size() > 1 ? asLong(stats.get(1)) : 0L;
        long activeFlagsCount = featureFlagService.countActiveFlags();
        return new GlobalStatsResponse(totalEvaluations, uniqueUsers, activeFlagsCount);
    }

    public FlagStatsResponse getFlagStats(String flagKey) {
        FeatureFlag featureFlag = featureFlagService.getByKey(flagKey);
        List<Object> stats = flatten(evaluationLogRepository.fetchFlagStats(featureFlag.getKey()));
        long totalEvaluations = stats.size() > 0 ? asLong(stats.get(0)) : 0L;
        long uniqueUsers = stats.size() > 1 ? asLong(stats.get(1)) : 0L;
        long trueCount = stats.size() > 2 ? asLong(stats.get(2)) : 0L;
        long falseCount = stats.size() > 3 ? asLong(stats.get(3)) : 0L;
        Instant lastEvaluatedAt = stats.size() > 4 ? asInstant(stats.get(4)) : null;
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
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof Boolean bool) {
            return bool ? 1L : 0L;
        }
        throw new IllegalArgumentException("Expected numeric value, got " + value.getClass());
    }

    private Instant asInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        throw new IllegalArgumentException("Expected Instant value, got " + value.getClass());
    }

    private List<Object> flatten(Object value) {
        List<Object> values = new ArrayList<>();
        flattenValue(value, values);
        return values;
    }

    private void flattenValue(Object value, List<Object> values) {
        if (value instanceof Object[] nested) {
            for (Object nestedValue : nested) {
                flattenValue(nestedValue, values);
            }
            return;
        }
        values.add(value);
    }
}

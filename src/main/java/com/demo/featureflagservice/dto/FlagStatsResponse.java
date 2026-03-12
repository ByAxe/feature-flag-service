package com.demo.featureflagservice.dto;

import java.time.Instant;

public record FlagStatsResponse(
        String flagKey,
        long totalEvaluations,
        long uniqueUsers,
        long trueCount,
        long falseCount,
        double trueRatio,
        double falseRatio,
        Instant lastEvaluatedAt) {
}

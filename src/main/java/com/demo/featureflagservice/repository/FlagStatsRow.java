package com.demo.featureflagservice.repository;

import java.time.Instant;

public record FlagStatsRow(
        long totalEvaluations,
        long uniqueUsers,
        Long trueCount,
        Long falseCount,
        Instant lastEvaluatedAt) {
}

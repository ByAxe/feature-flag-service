package com.demo.featureflagservice.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record FeatureFlagResponse(
        UUID id,
        String key,
        String description,
        boolean enabled,
        int rolloutPercentage,
        Set<String> targetUserIds,
        Instant createdAt,
        Instant updatedAt) {
}

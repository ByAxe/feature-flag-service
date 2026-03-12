package com.demo.featureflagservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;

public record FeatureFlagUpdateRequest(
        @Size(max = 512)
        String description,
        boolean enabled,
        @Min(0)
        @Max(100)
        int rolloutPercentage,
        Set<String> targetUserIds) {

    public Set<String> normalizedTargetUserIds() {
        if (targetUserIds == null) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String userId : targetUserIds) {
            if (userId != null) {
                String trimmed = userId.trim();
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }
        return normalized;
    }
}

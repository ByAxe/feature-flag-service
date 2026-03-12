package com.demo.featureflagservice.dto;

import com.demo.featureflagservice.util.NormalizationUtils;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record FeatureFlagUpdateRequest(
        @Size(max = 512)
        String description,
        boolean enabled,
        @Min(0)
        @Max(100)
        int rolloutPercentage,
        Set<@Size(max = 255) String> targetUserIds) {

    public Set<String> normalizedTargetUserIds() {
        return NormalizationUtils.normalizeUserIds(targetUserIds);
    }
}

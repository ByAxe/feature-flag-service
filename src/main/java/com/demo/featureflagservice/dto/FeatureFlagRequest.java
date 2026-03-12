package com.demo.featureflagservice.dto;

import com.demo.featureflagservice.util.NormalizationUtils;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record FeatureFlagRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9-]{1,64}$", message = "key must match [a-z0-9-] and be <= 64 chars")
        String key,
        @Size(max = 512)
        String description,
        boolean enabled,
        @Min(0)
        @Max(100)
        int rolloutPercentage,
        Set<@NotBlank @Size(max = 255) String> targetUserIds) {

    public Set<String> normalizedTargetUserIds() {
        return NormalizationUtils.normalizeUserIds(targetUserIds);
    }

    public String normalizedKey() {
        return NormalizationUtils.normalizeKey(key);
    }
}

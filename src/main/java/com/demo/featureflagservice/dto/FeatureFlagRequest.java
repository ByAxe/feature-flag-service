package com.demo.featureflagservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Locale;

public record FeatureFlagRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9-]{1,64}$", message = "key must match [a-z0-9-] and be <= 64 chars")
        String key,
        @Size(max = 512)
        String description,
        boolean enabled,
        @Min(0)
        @Max(100)
        int rolloutPercentage,
        Set<@NotBlank String> targetUserIds) {

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

    public String normalizedKey() {
        return key == null ? null : key.toLowerCase(Locale.ROOT);
    }
}

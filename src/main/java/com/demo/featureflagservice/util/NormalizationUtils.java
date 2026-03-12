package com.demo.featureflagservice.util;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class NormalizationUtils {

    private NormalizationUtils() {
    }

    public static String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static Set<String> normalizeUserIds(Set<String> values) {
        if (values == null) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String userId : values) {
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

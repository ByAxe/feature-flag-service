package com.demo.featureflagservice.repository;

import com.demo.featureflagservice.domain.FeatureFlag;
import java.util.Optional;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

    @Query("select f from FeatureFlag f where f.key = :key")
    Optional<FeatureFlag> findByCanonicalKey(@Param("key") String key);

    @Query("select (count(f) > 0) from FeatureFlag f where f.key = :key")
    boolean existsByCanonicalKey(@Param("key") String key);

    long countByEnabledTrue();

    default Optional<FeatureFlag> findByKey(String key) {
        return findByCanonicalKey(normalizeKey(key));
    }

    default boolean existsByKey(String key) {
        return existsByCanonicalKey(normalizeKey(key));
    }

    private static String normalizeKey(String key) {
        return key == null ? null : key.trim().toLowerCase(Locale.ROOT);
    }
}

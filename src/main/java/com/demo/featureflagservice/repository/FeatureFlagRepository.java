package com.demo.featureflagservice.repository;

import com.demo.featureflagservice.domain.FeatureFlag;
import com.demo.featureflagservice.util.NormalizationUtils;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

    @Query("select f from FeatureFlag f where f.key = :key")
    Optional<FeatureFlag> findByCanonicalKey(@Param("key") String key);

    @Query("select (count(f) > 0) from FeatureFlag f where f.key = :key")
    boolean existsByCanonicalKey(@Param("key") String key);

    @Query("select distinct f from FeatureFlag f left join fetch f.targetUserIds")
    List<FeatureFlag> findAllWithTargetUserIds();

    long countByEnabledTrue();

    default Optional<FeatureFlag> findByKey(String key) {
        return findByCanonicalKey(NormalizationUtils.normalizeKey(key));
    }

    default boolean existsByKey(String key) {
        return existsByCanonicalKey(NormalizationUtils.normalizeKey(key));
    }
}

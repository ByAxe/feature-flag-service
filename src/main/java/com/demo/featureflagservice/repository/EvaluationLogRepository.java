package com.demo.featureflagservice.repository;

import com.demo.featureflagservice.domain.EvaluationLog;
import java.time.Instant;
import java.util.Locale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluationLogRepository extends JpaRepository<EvaluationLog, Long> {

    @Query("""
        select count(e), count(distinct e.userId)
        from EvaluationLog e
        """)
    Object[] fetchGlobalStats();

    @Query("""
        select count(e),
               count(distinct e.userId),
               sum(case when e.result = true then 1 else 0 end),
               sum(case when e.result = false then 1 else 0 end),
               max(e.evaluatedAt)
        from EvaluationLog e
        where e.flagKey = :flagKey
        """)
    Object[] fetchFlagStatsByKey(@Param("flagKey") String flagKey);

    @Query("select (count(e) > 0) from EvaluationLog e where e.flagKey = :flagKey")
    boolean existsByCanonicalFlagKey(@Param("flagKey") String flagKey);

    @Query("select max(e.evaluatedAt) from EvaluationLog e where e.flagKey = :flagKey")
    Instant findLastEvaluatedAtByFlagKey(@Param("flagKey") String flagKey);

    default Object[] fetchFlagStats(String flagKey) {
        return fetchFlagStatsByKey(normalizeKey(flagKey));
    }

    default boolean existsByFlagKey(String flagKey) {
        return existsByCanonicalFlagKey(normalizeKey(flagKey));
    }

    default Instant findLastEvaluatedAt(String flagKey) {
        return findLastEvaluatedAtByFlagKey(normalizeKey(flagKey));
    }

    private static String normalizeKey(String key) {
        return key == null ? null : key.trim().toLowerCase(Locale.ROOT);
    }
}

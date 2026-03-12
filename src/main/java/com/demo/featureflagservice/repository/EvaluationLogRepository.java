package com.demo.featureflagservice.repository;

import com.demo.featureflagservice.domain.EvaluationLog;
import com.demo.featureflagservice.util.NormalizationUtils;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluationLogRepository extends JpaRepository<EvaluationLog, Long> {

    @Query("""
        select new com.demo.featureflagservice.repository.GlobalStatsRow(
            count(e),
            count(distinct e.userId)
        )
        from EvaluationLog e
        """)
    GlobalStatsRow fetchGlobalStats();

    @Query("""
        select new com.demo.featureflagservice.repository.FlagStatsRow(
            count(e),
            count(distinct e.userId),
            sum(case when e.result = true then 1 else 0 end),
            sum(case when e.result = false then 1 else 0 end),
            max(e.evaluatedAt)
        )
        from EvaluationLog e
        where e.flagKey = :flagKey
        """)
    FlagStatsRow fetchFlagStatsByKey(@Param("flagKey") String flagKey);

    @Query("select (count(e) > 0) from EvaluationLog e where e.flagKey = :flagKey")
    boolean existsByCanonicalFlagKey(@Param("flagKey") String flagKey);

    @Query("select max(e.evaluatedAt) from EvaluationLog e where e.flagKey = :flagKey")
    Instant findLastEvaluatedAtByFlagKey(@Param("flagKey") String flagKey);

    default FlagStatsRow fetchFlagStats(String flagKey) {
        return fetchFlagStatsByKey(NormalizationUtils.normalizeKey(flagKey));
    }

    default boolean existsByFlagKey(String flagKey) {
        return existsByCanonicalFlagKey(NormalizationUtils.normalizeKey(flagKey));
    }

    default Instant findLastEvaluatedAt(String flagKey) {
        return findLastEvaluatedAtByFlagKey(NormalizationUtils.normalizeKey(flagKey));
    }
}

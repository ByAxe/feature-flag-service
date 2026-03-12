package com.demo.featureflagservice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.demo.featureflagservice.domain.EvaluationLog;
import com.demo.featureflagservice.domain.FeatureFlag;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MigrationBackedDatabaseIT {

    @Autowired
    private FeatureFlagRepository featureFlagRepository;

    @Autowired
    private EvaluationLogRepository evaluationLogRepository;

    @Test
    void shouldPersistCanonicalizedFlagValuesAndDeduplicateTargetUsers() {
        String key = "Migrate-Case-" + UUID.randomUUID();
        FeatureFlag featureFlag = new FeatureFlag();
        featureFlag.setKey(key);
        featureFlag.setDescription("Migration smoke test");
        featureFlag.setEnabled(true);
        featureFlag.setRolloutPercentage(0);
        featureFlag.setTargetUserIds(Set.of(" user-a ", "user-a", "", "user-b", "user-b "));

        FeatureFlag saved = featureFlagRepository.saveAndFlush(featureFlag);

        assertAll(
                () -> assertThat(saved.getKey()).isEqualTo(key.toLowerCase()),
                () -> assertThat(saved.getTargetUserIds()).containsExactlyInAnyOrder("user-a", "user-b")
        );
    }

    @Test
    void shouldEnforceCanonicalUniquenessAtDatabaseLevel() {
        String key = "db-unique-" + UUID.randomUUID();
        FeatureFlag first = new FeatureFlag();
        first.setKey(key);
        first.setDescription("first");
        first.setEnabled(true);
        first.setRolloutPercentage(0);
        first.setTargetUserIds(Set.of());
        featureFlagRepository.saveAndFlush(first);

        FeatureFlag duplicate = new FeatureFlag();
        duplicate.setKey(key.toUpperCase());
        duplicate.setDescription("duplicate");
        duplicate.setEnabled(false);
        duplicate.setRolloutPercentage(0);
        duplicate.setTargetUserIds(Set.of());

        assertThatThrownBy(() -> featureFlagRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAggregateStatsFromPersistedEvaluationLogs() {
        String flagKey = "stats-migration-" + UUID.randomUUID();
        Instant base = Instant.now();
        evaluationLogRepository.deleteAll();

        evaluationLogRepository.saveAllAndFlush(List.of(
                evaluationLog(flagKey, "user-a", true, base),
                evaluationLog(flagKey, "user-b", false, base.plusSeconds(10)),
                evaluationLog(flagKey, "user-a", true, base.plusSeconds(20))
        ));

        List<Object> global = flatten(evaluationLogRepository.fetchGlobalStats());
        assertThat(asLong(global.get(0))).isEqualTo(3);
        assertThat(asLong(global.get(1))).isEqualTo(2);

        List<Object> stats = flatten(evaluationLogRepository.fetchFlagStats(flagKey.toUpperCase()));
        assertAll(
                () -> assertThat(asLong(stats.get(0))).isEqualTo(3),
                () -> assertThat(asLong(stats.get(1))).isEqualTo(2),
                () -> assertThat(asLong(stats.get(2))).isEqualTo(2),
                () -> assertThat(asLong(stats.get(3))).isEqualTo(1),
                () -> assertThat(stats.get(4)).isNotNull()
        );
    }

    private static List<Object> flatten(Object value) {
        List<Object> values = new ArrayList<>();
        flattenValue(value, values);
        return values;
    }

    private static void flattenValue(Object value, List<Object> values) {
        if (value instanceof Object[] nested) {
            for (Object nestedValue : nested) {
                flattenValue(nestedValue, values);
            }
        } else {
            values.add(value);
        }
    }

    private static long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof Boolean bool) {
            return bool ? 1L : 0L;
        }
        throw new IllegalArgumentException("Expected numeric value, got " + value.getClass());
    }

    private static EvaluationLog evaluationLog(String flagKey, String userId, boolean result, Instant at) {
        EvaluationLog log = new EvaluationLog();
        log.setFlagKey(flagKey);
        log.setUserId(userId);
        log.setResult(result);
        log.setReason(result ? "TRUE" : "FALSE");
        log.setEvaluatedAt(at);
        return log;
    }
}

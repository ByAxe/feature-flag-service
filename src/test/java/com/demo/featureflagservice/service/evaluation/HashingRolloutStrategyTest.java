package com.demo.featureflagservice.service.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class HashingRolloutStrategyTest {

    private final HashingRolloutStrategy strategy = new HashingRolloutStrategy();

    @Test
    void shouldBeDeterministicForSameInputs() {
        boolean first = strategy.isEnabled("new-checkout-flow", "user-42", 25);
        boolean second = strategy.isEnabled("new-checkout-flow", "user-42", 25);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void shouldDeterministicallyEvaluateAcrossManyInputs() {
        assertThat(strategy.isEnabled("rollout-1", "user-a", 42)).isEqualTo(strategy.isEnabled("rollout-1", "user-a", 42));
        assertThat(strategy.isEnabled("rollout-1", "user-b", 42)).isEqualTo(strategy.isEnabled("rollout-1", "user-b", 42));
        assertThat(strategy.isEnabled("rollout-2", "user-a", 42)).isEqualTo(strategy.isEnabled("rollout-2", "user-a", 42));
        assertThat(strategy.isEnabled("rollout-1", "user-a", 84)).isEqualTo(strategy.isEnabled("rollout-1", "user-a", 84));
    }

    @Test
    void shouldRespectRolloutBounds() {
        assertThat(strategy.isEnabled("flag", "user", 0)).isFalse();
        assertThat(strategy.isEnabled("flag", "user", 100)).isTrue();
    }

    @Test
    void shouldEvaluateFast() {
        assertTimeoutPreemptively(Duration.ofMillis(50), () -> strategy.isEnabled("flag", "user-42", 50));
    }

    @Test
    void shouldReturnRegressionValuesForKnownRolloutBuckets() {
        assertThat(strategy.isEnabled("deterministic-flag", "user-a", 42)).isFalse();
        assertThat(strategy.isEnabled("deterministic-flag", "user-b", 42)).isTrue();
        assertThat(strategy.isEnabled("new-checkout-flow", "qa-user", 80)).isTrue();
    }
}

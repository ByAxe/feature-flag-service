package com.demo.featureflagservice.service.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.demo.featureflagservice.domain.FeatureFlag;
import com.demo.featureflagservice.error.NotFoundException;
import com.demo.featureflagservice.logging.EvaluationAuditLogger;
import com.demo.featureflagservice.repository.EvaluationLogRepository;
import com.demo.featureflagservice.service.FeatureFlagService;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private FeatureFlagService featureFlagService;
    @Mock
    private HashingRolloutStrategy rolloutStrategy;
    @Mock
    private EvaluationLogRepository evaluationLogRepository;
    @Mock
    private EvaluationAuditLogger auditLogger;

    @InjectMocks
    private EvaluationService evaluationService;

    @Test
    void shouldEvaluateDisabledBeforeTargetListAndRollout() {
        FeatureFlag featureFlag = featureFlag("checkout-banner", false, 100, Set.of("user-1"));
        when(featureFlagService.getByKey("checkout-banner")).thenReturn(featureFlag);

        EvaluationResult result = evaluationService.evaluate("checkout-banner", "user-1");

        assertThat(result).isEqualTo(new EvaluationResult("checkout-banner", false, EvaluationReason.DISABLED));
    }

    @Test
    void shouldEvaluateTargetListBeforeRolloutBounds() {
        FeatureFlag featureFlag = featureFlag("checkout-banner", true, 0, Set.of("user-1"));
        when(featureFlagService.getByKey("checkout-banner")).thenReturn(featureFlag);

        EvaluationResult result = evaluationService.evaluate("checkout-banner", "user-1");

        assertThat(result).isEqualTo(new EvaluationResult("checkout-banner", true, EvaluationReason.TARGET_LIST));
    }

    @Test
    void shouldEvaluateFullRolloutBeforeNoRolloutAndPercentage() {
        FeatureFlag featureFlag = featureFlag("checkout-banner", true, 100, Set.of("user-2"));
        when(featureFlagService.getByKey("checkout-banner")).thenReturn(featureFlag);

        EvaluationResult result = evaluationService.evaluate("checkout-banner", "user-1");

        assertThat(result).isEqualTo(new EvaluationResult("checkout-banner", true, EvaluationReason.FULL_ROLLOUT));
    }

    @Test
    void shouldEvaluateNoRollout() {
        FeatureFlag featureFlag = featureFlag("checkout-banner", true, 0, Set.of("user-2"));
        when(featureFlagService.getByKey("checkout-banner")).thenReturn(featureFlag);

        EvaluationResult result = evaluationService.evaluate("checkout-banner", "user-1");

        assertThat(result).isEqualTo(new EvaluationResult("checkout-banner", false, EvaluationReason.NO_ROLLOUT));
    }

    @Test
    void shouldEvaluatePercentageRolloutForDeterministicBranch() {
        FeatureFlag featureFlag = featureFlag("checkout-banner", true, 42, Set.of("user-2"));
        when(featureFlagService.getByKey("checkout-banner")).thenReturn(featureFlag);
        when(rolloutStrategy.isEnabled("checkout-banner", "user-1", 42)).thenReturn(true);

        EvaluationResult result = evaluationService.evaluate("checkout-banner", "user-1");

        assertThat(result).isEqualTo(new EvaluationResult("checkout-banner", true, EvaluationReason.PERCENTAGE_ROLLOUT));
    }

    @Test
    void shouldRejectBlankUserIdForEvaluateAll() {
        assertThatThrownBy(() -> evaluationService.evaluateAll("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
    }

    @Test
    void shouldRejectBlankUserId() {
        assertThatThrownBy(() -> evaluationService.evaluate("checkout-banner", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
    }

    @Test
    void shouldFailWithNotFoundWhenFlagIsMissing() {
        when(featureFlagService.getByKey("checkout-banner"))
                .thenThrow(new NotFoundException("Flag 'checkout-banner' not found"));

        assertThatThrownBy(() -> evaluationService.evaluate("checkout-banner", "user-1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Flag 'checkout-banner' not found");
    }

    private static FeatureFlag featureFlag(String key, boolean enabled, int rolloutPercentage, Set<String> targetUserIds) {
        FeatureFlag featureFlag = new FeatureFlag();
        featureFlag.setKey(key);
        featureFlag.setEnabled(enabled);
        featureFlag.setRolloutPercentage(rolloutPercentage);
        featureFlag.setTargetUserIds(new LinkedHashSet<>(targetUserIds));
        return featureFlag;
    }
}

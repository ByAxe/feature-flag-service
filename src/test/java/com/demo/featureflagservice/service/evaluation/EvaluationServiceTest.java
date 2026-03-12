package com.demo.featureflagservice.service.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demo.featureflagservice.domain.FeatureFlag;
import com.demo.featureflagservice.error.NotFoundException;
import com.demo.featureflagservice.logging.EvaluationAuditLogger;
import com.demo.featureflagservice.repository.EvaluationLogRepository;
import com.demo.featureflagservice.service.FeatureFlagService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        FeatureFlag featureFlag = featureFlag("checkout-banner", true, 100, Set.of("user-1"));
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
    void shouldNormalizeUserAndFlagInputsBeforeEvaluation() {
        FeatureFlag featureFlag = featureFlag("trim-space-banner", true, 100, Set.of("user-1"));
        when(featureFlagService.getByKey("trim-space-banner")).thenReturn(featureFlag);

        EvaluationResult result = evaluationService.evaluate("  Trim-Space-Banner ", "  user-1  ", "eval-request-id");

        assertThat(result).isEqualTo(new EvaluationResult("trim-space-banner", true, EvaluationReason.TARGET_LIST));
        verify(featureFlagService).getByKey("trim-space-banner");
    }

    @Test
    void shouldRecordAuditRequestIdWhenEvaluateReceivesExplicitRequestId() {
        FeatureFlag featureFlag = featureFlag("checkout-banner", true, 100, Set.of());
        when(featureFlagService.getByKey("checkout-banner")).thenReturn(featureFlag);

        EvaluationResult result = evaluationService.evaluate("checkout-banner", "user-1", "request-id-123");

        assertThat(result).isEqualTo(new EvaluationResult("checkout-banner", true, EvaluationReason.FULL_ROLLOUT));
        verify(auditLogger).logSuccess(eq("request-id-123"), eq("user-1"), eq(result), anyLong());
    }

    @Test
    void shouldGenerateRequestIdWhenEvaluateRequestDoesNotIncludeOne() {
        FeatureFlag featureFlag = featureFlag("checkout-banner", true, 100, Set.of("user-1"));
        when(featureFlagService.getByKey("checkout-banner")).thenReturn(featureFlag);

        evaluationService.evaluate("checkout-banner", "user-1");

        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger).logSuccess(requestIdCaptor.capture(), eq("user-1"), any(EvaluationResult.class), anyLong());
        assertThat(requestIdCaptor.getValue()).isNotBlank();
    }

    @Test
    void shouldUseSingleRequestIdForEvaluateAll() {
        FeatureFlag fullRollout = featureFlag("checkout-banner", true, 100, Set.of("user-1"));
        FeatureFlag noRollout = featureFlag("beta-banner", true, 0, Set.of("user-1"));
        when(featureFlagService.listEntities()).thenReturn(List.of(fullRollout, noRollout));
        String userId = "user-1";

        List<EvaluationResult> results = evaluationService.evaluateAll(userId, "request-id-all");

        assertThat(results).hasSize(2);
        ArgumentCaptor<String> requestIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger, times(2)).logSuccess(requestIdCaptor.capture(), eq(userId), any(EvaluationResult.class), anyLong());
        assertThat(requestIdCaptor.getAllValues()).allMatch("request-id-all"::equals);
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
    void shouldRejectBlankFlagKey() {
        assertThatThrownBy(() -> evaluationService.evaluate("   ", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("flagKey is required");
    }

    @Test
    void shouldFailWithNotFoundWhenFlagIsMissing() {
        when(featureFlagService.getByKey("checkout-banner"))
                .thenThrow(new NotFoundException("Flag 'checkout-banner' not found"));

        assertThatThrownBy(() -> evaluationService.evaluate("checkout-banner", "user-1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Flag 'checkout-banner' not found");
    }

    @Test
    void shouldEvaluateDeterministicallyForSameInputsAtScaleWithinBudget() {
        FeatureFlag featureFlag = featureFlag("bench-flag", true, 50, Set.of("other"));
        when(featureFlagService.getByKey("bench-flag")).thenReturn(featureFlag);

        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
            for (int i = 0; i < 2_000; i++) {
                EvaluationResult result = evaluationService.evaluate("bench-flag", "user-1");
                assertThat(result.reason()).isEqualTo(EvaluationReason.PERCENTAGE_ROLLOUT);
            }
        });
        verify(rolloutStrategy, times(2_000)).isEnabled("bench-flag", "user-1", 50);
    }

    private static FeatureFlag featureFlag(String key, boolean enabled, int rolloutPercentage, Set<String> targetUserIds) {
        FeatureFlag featureFlag = new FeatureFlag();
        featureFlag.setKey(key);
        featureFlag.setDescription("desc");
        featureFlag.setEnabled(enabled);
        featureFlag.setRolloutPercentage(rolloutPercentage);
        featureFlag.setTargetUserIds(new LinkedHashSet<>(targetUserIds));
        return featureFlag;
    }

}

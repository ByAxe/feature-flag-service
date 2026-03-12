package com.demo.featureflagservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demo.featureflagservice.domain.FeatureFlag;
import com.demo.featureflagservice.dto.FeatureFlagRequest;
import com.demo.featureflagservice.dto.FeatureFlagResponse;
import com.demo.featureflagservice.dto.FeatureFlagUpdateRequest;
import com.demo.featureflagservice.error.ConflictException;
import com.demo.featureflagservice.error.NotFoundException;
import com.demo.featureflagservice.repository.FeatureFlagRepository;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @Mock
    private FeatureFlagRepository repository;

    @InjectMocks
    private FeatureFlagService service;

    @Test
    void shouldCreateCanonicalizedFlagWithNormalizedTargets() {
        FeatureFlagRequest request = new FeatureFlagRequest(
                "new-checkout-flow",
                "Enable fast path",
                true,
                30,
                targetUsers("  user-1 ", "", "user-1", "user-2 ", "USER-2"));

        when(repository.existsByCanonicalKey("new-checkout-flow")).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FeatureFlagResponse response = service.create(request);

        assertThat(response.key()).isEqualTo("new-checkout-flow");
        assertThat(response.enabled()).isTrue();
        assertThat(response.rolloutPercentage()).isEqualTo(30);
        assertThat(response.targetUserIds()).containsExactlyInAnyOrder("user-1", "user-2", "USER-2");
        verify(repository).existsByCanonicalKey("new-checkout-flow");
        verify(repository).save(any());
    }

    @Test
    void shouldRejectDuplicateCanonicalKeyOnCreate() {
        FeatureFlagRequest request = new FeatureFlagRequest("Dup-Flow", "desc", true, 10, Set.of("user-1"));

        when(repository.existsByCanonicalKey("dup-flow")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Flag with key 'dup-flow' already exists");

        verify(repository).existsByCanonicalKey("dup-flow");
        verify(repository, never()).save(any());
    }

    @Test
    void shouldUpdateExistingFlagWithoutChangingKey() {
        FeatureFlag existing = featureFlag("update-flow", false, 10, Set.of("legacy-user"));
        when(repository.findByCanonicalKey("update-flow")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FeatureFlagUpdateRequest request = new FeatureFlagUpdateRequest(
                "Updated description",
                true,
                90,
                targetUsers(" updated-user-1 ", "updated-user-2 "));

        FeatureFlagResponse response = service.update("UPDATE-FLOW", request);

        assertThat(response.key()).isEqualTo("update-flow");
        assertThat(response.description()).isEqualTo("Updated description");
        assertThat(response.enabled()).isTrue();
        assertThat(response.rolloutPercentage()).isEqualTo(90);
        assertThat(response.targetUserIds()).containsExactlyInAnyOrder("updated-user-1", "updated-user-2");
    }

    @Test
    void shouldReturnNotFoundForMissingFlagOnUpdate() {
        when(repository.findByCanonicalKey("missing-flag")).thenReturn(Optional.empty());

        FeatureFlagUpdateRequest request = new FeatureFlagUpdateRequest("desc", true, 0, Set.of());

        assertThatThrownBy(() -> service.update("missing-flag", request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Flag 'missing-flag' not found");
    }

    @Test
    void shouldReturnNotFoundForMissingFlagOnDelete() {
        when(repository.findByCanonicalKey("missing-delete")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("missing-delete"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Flag 'missing-delete' not found");
    }

    private static Set<String> targetUsers(String... values) {
        Set<String> valuesSet = new LinkedHashSet<>();
        for (String value : values) {
            valuesSet.add(value);
        }
        return valuesSet;
    }

    private static FeatureFlag featureFlag(String key, boolean enabled, int rolloutPercentage, Set<String> targetUserIds) {
        FeatureFlag featureFlag = new FeatureFlag();
        featureFlag.setKey(key);
        featureFlag.setDescription("desc");
        featureFlag.setEnabled(enabled);
        featureFlag.setRolloutPercentage(rolloutPercentage);
        featureFlag.setTargetUserIds(targetUserIds);
        return featureFlag;
    }
}

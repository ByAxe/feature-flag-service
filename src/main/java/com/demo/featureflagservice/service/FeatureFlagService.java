package com.demo.featureflagservice.service;

import com.demo.featureflagservice.domain.FeatureFlag;
import com.demo.featureflagservice.dto.FeatureFlagRequest;
import com.demo.featureflagservice.dto.FeatureFlagResponse;
import com.demo.featureflagservice.dto.FeatureFlagUpdateRequest;
import com.demo.featureflagservice.error.ConflictException;
import com.demo.featureflagservice.error.NotFoundException;
import com.demo.featureflagservice.repository.FeatureFlagRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FeatureFlagService {

    private final FeatureFlagRepository repository;

    public FeatureFlagService(FeatureFlagRepository repository) {
        this.repository = repository;
    }

    public FeatureFlagResponse create(FeatureFlagRequest request) {
        String canonicalKey = request.normalizedKey();
        if (repository.existsByKey(canonicalKey)) {
            throw new ConflictException("Flag with key '%s' already exists".formatted(canonicalKey));
        }

        FeatureFlag entity = new FeatureFlag();
        entity.setKey(canonicalKey);
        entity.setDescription(request.description());
        entity.setEnabled(request.enabled());
        entity.setRolloutPercentage(request.rolloutPercentage());
        entity.setTargetUserIds(request.normalizedTargetUserIds());
        return toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FeatureFlag getByKey(String key) {
        return repository.findByKey(canonicalizeKey(key))
                .orElseThrow(() -> new NotFoundException("Flag '%s' not found".formatted(key)));
    }

    @Transactional(readOnly = true)
    public FeatureFlagResponse getResponseByKey(String key) {
        return toResponse(getByKey(key));
    }

    public FeatureFlagResponse update(String key, FeatureFlagUpdateRequest request) {
        FeatureFlag entity = getByKey(key);
        entity.setDescription(request.description());
        entity.setEnabled(request.enabled());
        entity.setRolloutPercentage(request.rolloutPercentage());
        entity.setTargetUserIds(request.normalizedTargetUserIds());
        return toResponse(repository.save(entity));
    }

    public void delete(String key) {
        FeatureFlag entity = getByKey(key);
        repository.delete(entity);
    }

    public long countActiveFlags() {
        return repository.countByEnabledTrue();
    }

    private FeatureFlagResponse toResponse(FeatureFlag entity) {
        return new FeatureFlagResponse(
                entity.getId(),
                entity.getKey(),
                entity.getDescription(),
                entity.isEnabled(),
                entity.getRolloutPercentage(),
                entity.getTargetUserIds(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private String canonicalizeKey(String key) {
        return key == null ? null : key.trim().toLowerCase(Locale.ROOT);
    }
}

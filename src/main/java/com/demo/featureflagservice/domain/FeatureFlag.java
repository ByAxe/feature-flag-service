package com.demo.featureflagservice.domain;

import com.demo.featureflagservice.util.NormalizationUtils;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;

@Entity
@Table(name = "feature_flag")
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flag_key", nullable = false, unique = true, length = 64)
    private String key;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "rollout_percentage", nullable = false)
    private int rolloutPercentage;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "feature_flag_target_user", joinColumns = @JoinColumn(name = "feature_flag_id"))
    @Column(name = "target_user_id", nullable = false)
    private Set<String> targetUserIds = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        key = NormalizationUtils.normalizeKey(key);
        targetUserIds = NormalizationUtils.normalizeUserIds(targetUserIds);
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        key = NormalizationUtils.normalizeKey(key);
        targetUserIds = NormalizationUtils.normalizeUserIds(targetUserIds);
    }

    public UUID getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = NormalizationUtils.normalizeKey(key);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRolloutPercentage() {
        return rolloutPercentage;
    }

    public void setRolloutPercentage(int rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
    }

    public Set<String> getTargetUserIds() {
        return targetUserIds;
    }

    public void setTargetUserIds(Set<String> targetUserIds) {
        this.targetUserIds = NormalizationUtils.normalizeUserIds(targetUserIds);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

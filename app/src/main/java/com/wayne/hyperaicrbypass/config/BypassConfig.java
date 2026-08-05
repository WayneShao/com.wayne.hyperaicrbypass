package com.wayne.hyperaicrbypass.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class BypassConfig {
    private final boolean masterEnabled;
    private final boolean selectAllMode;
    private final EnumSet<Policy> selectedPolicies;
    private final long configRevision;
    private final long rescanGeneration;

    private BypassConfig(
            boolean masterEnabled,
            boolean selectAllMode,
            EnumSet<Policy> selectedPolicies,
            long configRevision,
            long rescanGeneration
    ) {
        this.masterEnabled = masterEnabled;
        this.selectAllMode = selectAllMode;
        this.selectedPolicies = selectedPolicies.clone();
        this.configRevision = configRevision;
        this.rescanGeneration = rescanGeneration;
    }

    public static BypassConfig defaults() {
        return new BypassConfig(true, true, EnumSet.allOf(Policy.class), 0L, 0L);
    }

    public static BypassConfig create(
            boolean masterEnabled,
            boolean selectAllMode,
            Set<Policy> selectedPolicies,
            long configRevision,
            long rescanGeneration
    ) {
        EnumSet<Policy> copy = selectedPolicies.isEmpty()
                ? EnumSet.noneOf(Policy.class)
                : EnumSet.copyOf(selectedPolicies);
        return new BypassConfig(
                masterEnabled, selectAllMode, copy, configRevision, rescanGeneration
        );
    }

    public boolean isMasterEnabled() {
        return masterEnabled;
    }

    public boolean isSelectAllMode() {
        return selectAllMode;
    }

    public boolean isSelected(Policy policy) {
        return selectedPolicies.contains(Objects.requireNonNull(policy));
    }

    public boolean shouldBypass(Policy policy) {
        return masterEnabled && isSelected(policy);
    }

    public Set<Policy> getSelectedPolicies() {
        return Collections.unmodifiableSet(selectedPolicies);
    }

    public long getConfigRevision() {
        return configRevision;
    }

    public long getRescanGeneration() {
        return rescanGeneration;
    }

    public BypassConfig withMaster(boolean enabled) {
        if (masterEnabled == enabled) {
            return this;
        }
        return new BypassConfig(
                enabled, selectAllMode, selectedPolicies, configRevision + 1, rescanGeneration
        );
    }

    public BypassConfig withPolicy(Policy policy, boolean selected) {
        Objects.requireNonNull(policy);
        if (selectedPolicies.contains(policy) == selected) {
            return this;
        }
        EnumSet<Policy> updated = selectedPolicies.clone();
        if (selected) {
            updated.add(policy);
        } else {
            updated.remove(policy);
        }
        return new BypassConfig(
                masterEnabled, selectAllMode, updated, configRevision + 1, rescanGeneration
        );
    }

    public BypassConfig withAllPolicies(boolean selected) {
        EnumSet<Policy> updated = selected
                ? EnumSet.allOf(Policy.class)
                : EnumSet.noneOf(Policy.class);
        if (updated.equals(selectedPolicies)) {
            return this;
        }
        return new BypassConfig(
                masterEnabled, selected, updated, configRevision + 1, rescanGeneration
        );
    }

    public BypassConfig withSelectAllMode(boolean enabled) {
        EnumSet<Policy> updated = enabled
                ? EnumSet.allOf(Policy.class)
                : selectedPolicies.clone();
        if (selectAllMode == enabled && updated.equals(selectedPolicies)) {
            return this;
        }
        return new BypassConfig(
                masterEnabled, enabled, updated, configRevision + 1, rescanGeneration
        );
    }

    public BypassConfig nextRescanGeneration() {
        return new BypassConfig(
                masterEnabled, selectAllMode, selectedPolicies, configRevision, rescanGeneration + 1
        );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BypassConfig that)) {
            return false;
        }
        return masterEnabled == that.masterEnabled
                && selectAllMode == that.selectAllMode
                && configRevision == that.configRevision
                && rescanGeneration == that.rescanGeneration
                && selectedPolicies.equals(that.selectedPolicies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                masterEnabled, selectAllMode, selectedPolicies, configRevision, rescanGeneration
        );
    }
}

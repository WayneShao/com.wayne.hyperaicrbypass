package com.wayne.hyperaicrbypass.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class BypassConfig {
    private final OperatingMode mode;
    private final boolean powerExceptionEnabled;
    private final ProgressPrecision progressPrecision;
    private final ProgressPrecision lastNonOriginalPrecision;
    private final boolean selectAllMode;
    private final EnumSet<Policy> selectedPolicies;
    private final long configRevision;
    private final long rescanGeneration;

    private BypassConfig(
            OperatingMode mode,
            boolean powerExceptionEnabled,
            ProgressPrecision progressPrecision,
            ProgressPrecision lastNonOriginalPrecision,
            boolean selectAllMode,
            EnumSet<Policy> selectedPolicies,
            long configRevision,
            long rescanGeneration
    ) {
        this.mode = Objects.requireNonNull(mode);
        this.powerExceptionEnabled = powerExceptionEnabled;
        this.progressPrecision = Objects.requireNonNull(progressPrecision);
        this.lastNonOriginalPrecision = requireNonOriginal(lastNonOriginalPrecision);
        this.selectAllMode = selectAllMode;
        this.selectedPolicies = selectedPolicies.clone();
        this.configRevision = configRevision;
        this.rescanGeneration = rescanGeneration;
    }

    public static BypassConfig defaults() {
        return new BypassConfig(
                OperatingMode.BYPASS,
                false,
                ProgressPrecision.THOUSANDTHS,
                ProgressPrecision.THOUSANDTHS,
                true,
                EnumSet.allOf(Policy.class),
                0L,
                0L
        );
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
                masterEnabled ? OperatingMode.BYPASS : OperatingMode.NORMAL,
                false,
                ProgressPrecision.THOUSANDTHS,
                ProgressPrecision.THOUSANDTHS,
                selectAllMode,
                copy,
                configRevision,
                rescanGeneration
        );
    }

    public static BypassConfig create(
            OperatingMode mode,
            boolean powerExceptionEnabled,
            ProgressPrecision progressPrecision,
            ProgressPrecision lastNonOriginalPrecision,
            boolean selectAllMode,
            Set<Policy> selectedPolicies,
            long configRevision,
            long rescanGeneration
    ) {
        EnumSet<Policy> copy = selectedPolicies.isEmpty()
                ? EnumSet.noneOf(Policy.class)
                : EnumSet.copyOf(selectedPolicies);
        return new BypassConfig(
                mode,
                powerExceptionEnabled,
                progressPrecision,
                lastNonOriginalPrecision,
                selectAllMode,
                copy,
                configRevision,
                rescanGeneration
        );
    }

    public OperatingMode getMode() {
        return mode;
    }

    public boolean isPowerExceptionEnabled() {
        return powerExceptionEnabled;
    }

    public ProgressPrecision getProgressPrecision() {
        return progressPrecision;
    }

    public ProgressPrecision getLastNonOriginalPrecision() {
        return lastNonOriginalPrecision;
    }

    public boolean isMasterEnabled() {
        return mode == OperatingMode.BYPASS;
    }

    public boolean isSelectAllMode() {
        return selectAllMode;
    }

    public boolean isSelected(Policy policy) {
        return selectedPolicies.contains(Objects.requireNonNull(policy));
    }

    public boolean shouldBypass(Policy policy) {
        return mode == OperatingMode.BYPASS && isSelected(policy);
    }

    public boolean shouldBypass(Policy policy, boolean externalPowerConnected) {
        return effectiveMode(externalPowerConnected) == OperatingMode.BYPASS && isSelected(policy);
    }

    public OperatingMode effectiveMode(boolean externalPowerConnected) {
        return mode.effective(powerExceptionEnabled, externalPowerConnected);
    }

    public boolean shouldPause(boolean externalPowerConnected) {
        return effectiveMode(externalPowerConnected) == OperatingMode.POWER_SAVE;
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
        return withMode(enabled ? OperatingMode.BYPASS : OperatingMode.NORMAL);
    }

    public BypassConfig withMode(OperatingMode updatedMode) {
        Objects.requireNonNull(updatedMode);
        if (mode == updatedMode) {
            return this;
        }
        return copy(updatedMode, powerExceptionEnabled, progressPrecision,
                lastNonOriginalPrecision, selectAllMode, selectedPolicies, configRevision + 1,
                rescanGeneration);
    }

    public BypassConfig withPowerException(boolean enabled) {
        if (powerExceptionEnabled == enabled) {
            return this;
        }
        return copy(mode, enabled, progressPrecision, lastNonOriginalPrecision, selectAllMode,
                selectedPolicies, configRevision + 1, rescanGeneration);
    }

    public BypassConfig withProgressPrecision(ProgressPrecision precision) {
        Objects.requireNonNull(precision);
        ProgressPrecision updatedLast = precision.isPrecise()
                ? precision
                : lastNonOriginalPrecision;
        if (progressPrecision == precision && lastNonOriginalPrecision == updatedLast) {
            return this;
        }
        return copy(mode, powerExceptionEnabled, precision, updatedLast, selectAllMode,
                selectedPolicies, configRevision + 1, rescanGeneration);
    }

    public BypassConfig withPreciseProgress(boolean enabled) {
        return withProgressPrecision(enabled ? lastNonOriginalPrecision : ProgressPrecision.ORIGINAL);
    }

    public BypassConfig withLastNonOriginalPrecision(ProgressPrecision precision) {
        requireNonOriginal(precision);
        if (lastNonOriginalPrecision == precision) {
            return this;
        }
        ProgressPrecision active = progressPrecision.isPrecise() ? precision : progressPrecision;
        return copy(mode, powerExceptionEnabled, active, precision, selectAllMode,
                selectedPolicies, configRevision + 1, rescanGeneration);
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
        return copy(mode, powerExceptionEnabled, progressPrecision, lastNonOriginalPrecision,
                selectAllMode, updated, configRevision + 1, rescanGeneration);
    }

    public BypassConfig withAllPolicies(boolean selected) {
        EnumSet<Policy> updated = selected
                ? EnumSet.allOf(Policy.class)
                : EnumSet.noneOf(Policy.class);
        if (updated.equals(selectedPolicies)) {
            return this;
        }
        return copy(mode, powerExceptionEnabled, progressPrecision, lastNonOriginalPrecision,
                selected, updated, configRevision + 1, rescanGeneration);
    }

    public BypassConfig withSelectAllMode(boolean enabled) {
        EnumSet<Policy> updated = enabled
                ? EnumSet.allOf(Policy.class)
                : selectedPolicies.clone();
        if (selectAllMode == enabled && updated.equals(selectedPolicies)) {
            return this;
        }
        return copy(mode, powerExceptionEnabled, progressPrecision, lastNonOriginalPrecision,
                enabled, updated, configRevision + 1, rescanGeneration);
    }

    public BypassConfig nextRescanGeneration() {
        return copy(mode, powerExceptionEnabled, progressPrecision, lastNonOriginalPrecision,
                selectAllMode, selectedPolicies, configRevision, rescanGeneration + 1);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BypassConfig that)) {
            return false;
        }
        return mode == that.mode
                && powerExceptionEnabled == that.powerExceptionEnabled
                && progressPrecision == that.progressPrecision
                && lastNonOriginalPrecision == that.lastNonOriginalPrecision
                && selectAllMode == that.selectAllMode
                && configRevision == that.configRevision
                && rescanGeneration == that.rescanGeneration
                && selectedPolicies.equals(that.selectedPolicies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mode, powerExceptionEnabled, progressPrecision, lastNonOriginalPrecision,
                selectAllMode, selectedPolicies, configRevision, rescanGeneration
        );
    }

    private static ProgressPrecision requireNonOriginal(ProgressPrecision precision) {
        Objects.requireNonNull(precision);
        if (!precision.isPrecise()) {
            throw new IllegalArgumentException("Last precision must have at least one decimal");
        }
        return precision;
    }

    private static BypassConfig copy(
            OperatingMode mode,
            boolean powerExceptionEnabled,
            ProgressPrecision progressPrecision,
            ProgressPrecision lastNonOriginalPrecision,
            boolean selectAllMode,
            Set<Policy> selectedPolicies,
            long configRevision,
            long rescanGeneration
    ) {
        EnumSet<Policy> copy = selectedPolicies.isEmpty()
                ? EnumSet.noneOf(Policy.class)
                : EnumSet.copyOf(selectedPolicies);
        return new BypassConfig(mode, powerExceptionEnabled, progressPrecision,
                lastNonOriginalPrecision, selectAllMode, copy, configRevision, rescanGeneration);
    }
}

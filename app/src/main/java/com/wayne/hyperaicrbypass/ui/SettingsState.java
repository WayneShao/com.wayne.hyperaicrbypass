package com.wayne.hyperaicrbypass.ui;

import com.wayne.hyperaicrbypass.adapt.CoverageLayer;
import com.wayne.hyperaicrbypass.config.BypassConfig;
import com.wayne.hyperaicrbypass.config.Policy;
import com.wayne.hyperaicrbypass.config.OperatingMode;
import com.wayne.hyperaicrbypass.config.ProgressPrecision;
import com.wayne.hyperaicrbypass.hook.ExecutionCoverage;

import java.util.Map;
import java.util.Objects;

public final class SettingsState {
    private final BypassConfig config;
    private final String aicrVersion;
    private final Map<Policy, CoverageLayer> coverage;
    private final ExecutionCoverage executionCoverage;
    private final boolean externalPowerConnected;

    public SettingsState(
            BypassConfig config,
            String aicrVersion,
            Map<Policy, CoverageLayer> coverage
    ) {
        this(config, aicrVersion, coverage, ExecutionCoverage.PENDING, false);
    }

    public SettingsState(
            BypassConfig config,
            String aicrVersion,
            Map<Policy, CoverageLayer> coverage,
            ExecutionCoverage executionCoverage,
            boolean externalPowerConnected
    ) {
        this.config = Objects.requireNonNull(config);
        this.aicrVersion = Objects.requireNonNull(aicrVersion);
        this.coverage = Map.copyOf(coverage);
        this.executionCoverage = Objects.requireNonNull(executionCoverage);
        this.externalPowerConnected = externalPowerConnected;
    }

    public boolean isMasterEnabled() {
        return config.isMasterEnabled();
    }

    public boolean isBypassEnabled() {
        return config.getMode() == OperatingMode.BYPASS;
    }

    public boolean isPowerSaveEnabled() {
        return config.getMode() == OperatingMode.POWER_SAVE;
    }

    public boolean isPowerSaveToggleEnabled() {
        return isPowerSaveEnabled() || executionCoverage == ExecutionCoverage.AVAILABLE;
    }

    public boolean isPowerExceptionEnabled() {
        return config.isPowerExceptionEnabled();
    }

    public boolean isPowerExceptionEditable() {
        return isPowerSaveEnabled();
    }

    public boolean isPreciseProgressEnabled() {
        return config.getProgressPrecision().isPrecise();
    }

    public boolean isPrecisionSelectorEnabled() {
        return isPreciseProgressEnabled();
    }

    public ProgressPrecision selectedPrecision() {
        return isPreciseProgressEnabled()
                ? config.getProgressPrecision()
                : config.getLastNonOriginalPrecision();
    }

    public String runtimeSummary() {
        if (isBypassEnabled()) {
            return "按已选门槛绕过";
        }
        if (!isPowerSaveEnabled()) {
            return "按 AICR 原逻辑运行";
        }
        if (executionCoverage == ExecutionCoverage.PENDING) {
            return "正在确认暂停链";
        }
        if (executionCoverage == ExecutionCoverage.UNAVAILABLE) {
            return "暂停链适配失败";
        }
        if (config.isPowerExceptionEnabled() && externalPowerConnected) {
            return "外部供电中，按已选门槛绕过";
        }
        return "AICR 已暂停";
    }

    public boolean isPolicySelected(Policy policy) {
        return config.isSelected(policy);
    }

    public boolean isSelectAllMode() {
        return config.isSelectAllMode();
    }

    public boolean isPolicyEditable(Policy policy) {
        CoverageLayer layer = coverage.get(policy);
        boolean unavailable = layer == CoverageLayer.UNAVAILABLE;
        return !config.isSelectAllMode() && !unavailable;
    }

    public boolean areAllPoliciesSelected() {
        return config.getSelectedPolicies().size() == Policy.values().length;
    }

    public String selectionSummary() {
        return config.getSelectedPolicies().size() + " / " + Policy.values().length;
    }

    public String versionSummary() {
        return "unknown".equals(aicrVersion) ? "AICR not installed" : "AICR " + aicrVersion;
    }

    public String rescanSummary() {
        return "Rescan " + config.getRescanGeneration();
    }

    public String coverageLabel(Policy policy) {
        CoverageLayer layer = coverage.getOrDefault(policy, CoverageLayer.PENDING);
        return switch (layer) {
            case PENDING -> "Pending";
            case EXACT -> "Exact";
            case SEMANTIC -> "Adapted";
            case FALLBACK -> "Fallback";
            case PARTIAL -> "Partial";
            case UNAVAILABLE -> "Unavailable";
        };
    }
}

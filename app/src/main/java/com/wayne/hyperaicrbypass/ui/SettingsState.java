package com.wayne.hyperaicrbypass.ui;

import com.wayne.hyperaicrbypass.adapt.CoverageLayer;
import com.wayne.hyperaicrbypass.config.BypassConfig;
import com.wayne.hyperaicrbypass.config.Policy;
import com.wayne.hyperaicrbypass.config.OperatingMode;
import com.wayne.hyperaicrbypass.config.ProgressPrecision;
import com.wayne.hyperaicrbypass.hook.ExecutionCoverage;
import com.wayne.hyperaicrbypass.hook.PreciseProgressCoverage;

import java.util.Map;
import java.util.Objects;

public final class SettingsState {
    private final BypassConfig config;
    private final String aicrVersion;
    private final Map<Policy, CoverageLayer> coverage;
    private final ExecutionCoverage executionCoverage;
    private final boolean externalPowerConnected;
    private final PreciseProgressCoverage preciseProgressCoverage;
    private final int preciseProgressCount;
    private final int preciseProgressExpected;

    public SettingsState(
            BypassConfig config,
            String aicrVersion,
            Map<Policy, CoverageLayer> coverage
    ) {
        this(config, aicrVersion, coverage, ExecutionCoverage.PENDING, false,
                PreciseProgressCoverage.PENDING, 0);
    }

    public SettingsState(
            BypassConfig config,
            String aicrVersion,
            Map<Policy, CoverageLayer> coverage,
            ExecutionCoverage executionCoverage,
            boolean externalPowerConnected
    ) {
        this(config, aicrVersion, coverage, executionCoverage, externalPowerConnected,
                PreciseProgressCoverage.PENDING, 0);
    }

    public SettingsState(
            BypassConfig config,
            String aicrVersion,
            Map<Policy, CoverageLayer> coverage,
            ExecutionCoverage executionCoverage,
            boolean externalPowerConnected,
            PreciseProgressCoverage preciseProgressCoverage,
            int preciseProgressCount
    ) {
        this(config, aicrVersion, coverage, executionCoverage, externalPowerConnected,
                preciseProgressCoverage, preciseProgressCount, 4);
    }

    public SettingsState(
            BypassConfig config,
            String aicrVersion,
            Map<Policy, CoverageLayer> coverage,
            ExecutionCoverage executionCoverage,
            boolean externalPowerConnected,
            PreciseProgressCoverage preciseProgressCoverage,
            int preciseProgressCount,
            int preciseProgressExpected
    ) {
        this.config = Objects.requireNonNull(config);
        this.aicrVersion = Objects.requireNonNull(aicrVersion);
        this.coverage = Map.copyOf(coverage);
        this.executionCoverage = Objects.requireNonNull(executionCoverage);
        this.externalPowerConnected = externalPowerConnected;
        this.preciseProgressCoverage = Objects.requireNonNull(preciseProgressCoverage);
        this.preciseProgressCount = preciseProgressCount;
        this.preciseProgressExpected = preciseProgressExpected;
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
        return isPreciseProgressEnabled() && isPreciseProgressToggleEnabled();
    }

    public boolean isPreciseProgressToggleEnabled() {
        return isPreciseProgressEnabled()
                || preciseProgressCoverage == PreciseProgressCoverage.AVAILABLE;
    }

    public String preciseProgressStatus() {
        return switch (preciseProgressCoverage) {
            case PENDING -> "等待 AICR 进程上报";
            case AVAILABLE -> "可用 · 动态适配 " + preciseProgressCount + " / "
                    + preciseProgressExpected;
            case PARTIAL -> "部分可用 · " + preciseProgressCount + " / "
                    + preciseProgressExpected;
            case UNAVAILABLE -> "不可用 · 当前版本未适配";
        };
    }

    public CoverageLayer preciseProgressLayer() {
        return switch (preciseProgressCoverage) {
            case PENDING -> CoverageLayer.PENDING;
            case AVAILABLE -> CoverageLayer.SEMANTIC;
            case PARTIAL -> CoverageLayer.PARTIAL;
            case UNAVAILABLE -> CoverageLayer.UNAVAILABLE;
        };
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
        CoverageLayer layer = coverage.getOrDefault(policy, CoverageLayer.PENDING);
        boolean available = layer != CoverageLayer.PENDING
                && layer != CoverageLayer.UNAVAILABLE;
        return !config.isSelectAllMode() && (available || config.isSelected(policy));
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
        long available = coverage.values().stream()
                .filter(layer -> layer != CoverageLayer.PENDING
                        && layer != CoverageLayer.UNAVAILABLE)
                .count();
        return "已适配 " + available + " / " + Policy.values().length
                + " · 扫描 " + config.getRescanGeneration();
    }

    public String coverageLabel(Policy policy) {
        CoverageLayer layer = coverage.getOrDefault(policy, CoverageLayer.PENDING);
        return switch (layer) {
            case PENDING -> "等待 AICR 进程上报";
            case EXACT -> "可用 · 精确适配";
            case SEMANTIC -> "可用 · 动态适配";
            case FALLBACK -> "可用 · 兼容回退";
            case PARTIAL -> "部分可用";
            case UNAVAILABLE -> "不可用 · 当前版本未适配";
        };
    }

    public CoverageLayer coverageLayer(Policy policy) {
        return coverage.getOrDefault(policy, CoverageLayer.PENDING);
    }
}

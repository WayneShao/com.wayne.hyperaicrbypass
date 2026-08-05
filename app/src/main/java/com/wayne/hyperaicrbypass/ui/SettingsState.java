package com.wayne.hyperaicrbypass.ui;

import com.wayne.hyperaicrbypass.adapt.CoverageLayer;
import com.wayne.hyperaicrbypass.config.BypassConfig;
import com.wayne.hyperaicrbypass.config.Policy;

import java.util.Map;
import java.util.Objects;

public final class SettingsState {
    private final BypassConfig config;
    private final String aicrVersion;
    private final Map<Policy, CoverageLayer> coverage;

    public SettingsState(
            BypassConfig config,
            String aicrVersion,
            Map<Policy, CoverageLayer> coverage
    ) {
        this.config = Objects.requireNonNull(config);
        this.aicrVersion = Objects.requireNonNull(aicrVersion);
        this.coverage = Map.copyOf(coverage);
    }

    public boolean isMasterEnabled() {
        return config.isMasterEnabled();
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

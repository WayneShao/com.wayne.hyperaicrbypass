package com.wayne.hyperaicrbypass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wayne.hyperaicrbypass.adapt.CoverageLayer;
import com.wayne.hyperaicrbypass.config.BypassConfig;
import com.wayne.hyperaicrbypass.config.Policy;
import com.wayne.hyperaicrbypass.config.OperatingMode;
import com.wayne.hyperaicrbypass.config.ProgressPrecision;
import com.wayne.hyperaicrbypass.hook.ExecutionCoverage;

import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

public class SettingsStateTest {
    @Test
    public void summarySeparatesMasterAndChildSelection() {
        SettingsState all = new SettingsState(BypassConfig.defaults(), "4.0.6", Map.of());
        assertTrue(all.isMasterEnabled());
        assertTrue(all.isSelectAllMode());
        assertTrue(all.areAllPoliciesSelected());
        assertFalse(all.isPolicyEditable(Policy.TEMPERATURE));
        assertEquals("11 / 11", all.selectionSummary());
        assertEquals("AICR 4.0.6", all.versionSummary());

        BypassConfig partial = BypassConfig.defaults()
                .withSelectAllMode(false)
                .withPolicy(Policy.CHARGING, false)
                .withMaster(false);
        SettingsState disabled = new SettingsState(partial, "unknown", Map.of());
        assertFalse(disabled.isMasterEnabled());
        assertFalse(disabled.areAllPoliciesSelected());
        assertTrue(disabled.isPolicyEditable(Policy.TEMPERATURE));
        assertEquals("10 / 11", disabled.selectionSummary());
        assertEquals("AICR not installed", disabled.versionSummary());
    }

    @Test
    public void coverageLabelsExposeEveryAdaptationLayer() {
        EnumMap<Policy, CoverageLayer> reports = new EnumMap<>(Policy.class);
        reports.put(Policy.TEMPERATURE, CoverageLayer.EXACT);
        reports.put(Policy.CHARGING, CoverageLayer.SEMANTIC);
        reports.put(Policy.POWER, CoverageLayer.FALLBACK);
        reports.put(Policy.SCREEN_IDLE, CoverageLayer.UNAVAILABLE);
        reports.put(Policy.MIGRATION, CoverageLayer.PARTIAL);

        BypassConfig generationFour = BypassConfig.defaults()
                .withSelectAllMode(false)
                .nextRescanGeneration()
                .nextRescanGeneration()
                .nextRescanGeneration()
                .nextRescanGeneration();
        SettingsState state = new SettingsState(generationFour, "4.0.6", reports);

        assertEquals("Exact", state.coverageLabel(Policy.TEMPERATURE));
        assertEquals("Adapted", state.coverageLabel(Policy.CHARGING));
        assertEquals("Fallback", state.coverageLabel(Policy.POWER));
        assertEquals("Unavailable", state.coverageLabel(Policy.SCREEN_IDLE));
        assertFalse(state.isPolicyEditable(Policy.SCREEN_IDLE));
        assertEquals("Partial", state.coverageLabel(Policy.MIGRATION));
        assertTrue(state.isPolicyEditable(Policy.MIGRATION));
        assertEquals("Rescan 4", state.rescanSummary());
    }

    @Test
    public void runtimeSwitchesAreMutuallyExclusiveAndCanBothBeOff() {
        SettingsState bypass = state(
                BypassConfig.defaults(), ExecutionCoverage.AVAILABLE, false);
        assertTrue(bypass.isBypassEnabled());
        assertFalse(bypass.isPowerSaveEnabled());

        SettingsState powerSave = state(
                BypassConfig.defaults().withMode(OperatingMode.POWER_SAVE),
                ExecutionCoverage.AVAILABLE,
                false
        );
        assertFalse(powerSave.isBypassEnabled());
        assertTrue(powerSave.isPowerSaveEnabled());

        SettingsState normal = state(
                BypassConfig.defaults().withMode(OperatingMode.NORMAL),
                ExecutionCoverage.AVAILABLE,
                false
        );
        assertFalse(normal.isBypassEnabled());
        assertFalse(normal.isPowerSaveEnabled());
        assertEquals("按 AICR 原逻辑运行", normal.runtimeSummary());
    }

    @Test
    public void unavailableCoverageBlocksNewPowerSaveButAllowsTurningExistingModeOff() {
        SettingsState normal = state(
                BypassConfig.defaults().withMode(OperatingMode.NORMAL),
                ExecutionCoverage.UNAVAILABLE,
                false
        );
        assertFalse(normal.isPowerSaveToggleEnabled());

        SettingsState active = state(
                BypassConfig.defaults().withMode(OperatingMode.POWER_SAVE),
                ExecutionCoverage.UNAVAILABLE,
                false
        );
        assertTrue(active.isPowerSaveToggleEnabled());
        assertEquals("暂停链适配失败", active.runtimeSummary());
    }

    @Test
    public void poweredExceptionUsesSelectedBypassesAndUpdatesSummary() {
        BypassConfig config = BypassConfig.defaults()
                .withMode(OperatingMode.POWER_SAVE)
                .withPowerException(true);

        SettingsState unplugged = state(config, ExecutionCoverage.AVAILABLE, false);
        assertTrue(unplugged.isPowerExceptionEditable());
        assertEquals("AICR 已暂停", unplugged.runtimeSummary());

        SettingsState powered = state(config, ExecutionCoverage.AVAILABLE, true);
        assertEquals("外部供电中，按已选门槛绕过", powered.runtimeSummary());
    }

    @Test
    public void progressSwitchAndPrecisionSelectionAreIndependent() {
        BypassConfig original = BypassConfig.defaults()
                .withProgressPrecision(ProgressPrecision.HUNDREDTHS)
                .withPreciseProgress(false)
                .withMode(OperatingMode.NORMAL);
        SettingsState disabled = state(original, ExecutionCoverage.AVAILABLE, false);

        assertFalse(disabled.isPreciseProgressEnabled());
        assertFalse(disabled.isPrecisionSelectorEnabled());
        assertEquals(ProgressPrecision.HUNDREDTHS, disabled.selectedPrecision());

        SettingsState enabled = state(
                original.withPreciseProgress(true), ExecutionCoverage.AVAILABLE, false);
        assertTrue(enabled.isPreciseProgressEnabled());
        assertTrue(enabled.isPrecisionSelectorEnabled());
        assertEquals(ProgressPrecision.HUNDREDTHS, enabled.selectedPrecision());
    }

    private static SettingsState state(
            BypassConfig config,
            ExecutionCoverage executionCoverage,
            boolean externalPowerConnected
    ) {
        return new SettingsState(
                config, "4.0.6", Map.of(), executionCoverage, externalPowerConnected
        );
    }
}

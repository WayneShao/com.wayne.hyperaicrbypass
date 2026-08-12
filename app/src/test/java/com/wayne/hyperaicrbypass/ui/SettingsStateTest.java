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
import com.wayne.hyperaicrbypass.hook.PreciseProgressCoverage;

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

        assertEquals("可用 · 精确适配", state.coverageLabel(Policy.TEMPERATURE));
        assertEquals("可用 · 动态适配", state.coverageLabel(Policy.CHARGING));
        assertEquals("可用 · 兼容回退", state.coverageLabel(Policy.POWER));
        assertEquals("不可用 · 当前版本未适配", state.coverageLabel(Policy.SCREEN_IDLE));
        assertTrue(state.isPolicyEditable(Policy.SCREEN_IDLE));
        assertEquals("部分可用", state.coverageLabel(Policy.MIGRATION));
        assertTrue(state.isPolicyEditable(Policy.MIGRATION));
        assertEquals("已适配 4 / 11 · 扫描 4", state.rescanSummary());
        assertTrue(state.isPolicyEditable(Policy.DAILY_COUNT));
        assertEquals("等待 AICR 进程上报", state.coverageLabel(Policy.DAILY_COUNT));
    }

    @Test
    public void unavailableSelectedPolicyCanBeTurnedOffButCannotBeTurnedOn() {
        BypassConfig selected = BypassConfig.defaults().withSelectAllMode(false);
        SettingsState active = new SettingsState(
                selected, "3.63.0", Map.of(Policy.TEMPERATURE, CoverageLayer.UNAVAILABLE));
        assertTrue(active.isPolicyEditable(Policy.TEMPERATURE));

        SettingsState inactive = new SettingsState(
                selected.withPolicy(Policy.TEMPERATURE, false),
                "3.63.0", Map.of(Policy.TEMPERATURE, CoverageLayer.UNAVAILABLE));
        assertFalse(inactive.isPolicyEditable(Policy.TEMPERATURE));
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

    @Test
    public void preciseProgressStatusReflectsItsOwnFourHookChain() {
        SettingsState available = new SettingsState(
                BypassConfig.defaults(), "3.63.0", Map.of(),
                ExecutionCoverage.AVAILABLE, false,
                PreciseProgressCoverage.AVAILABLE, 4
        );
        SettingsState partial = new SettingsState(
                BypassConfig.defaults().withPreciseProgress(false), "3.63.0", Map.of(),
                ExecutionCoverage.AVAILABLE, false,
                PreciseProgressCoverage.PARTIAL, 2
        );

        assertEquals("可用 · 动态适配 4 / 4", available.preciseProgressStatus());
        assertTrue(available.isPreciseProgressToggleEnabled());
        assertEquals("部分可用 · 2 / 4", partial.preciseProgressStatus());
        assertFalse(partial.isPreciseProgressToggleEnabled());
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

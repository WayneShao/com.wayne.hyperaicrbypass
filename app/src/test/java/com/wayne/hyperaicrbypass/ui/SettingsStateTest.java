package com.wayne.hyperaicrbypass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wayne.hyperaicrbypass.adapt.CoverageLayer;
import com.wayne.hyperaicrbypass.config.BypassConfig;
import com.wayne.hyperaicrbypass.config.Policy;

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
}

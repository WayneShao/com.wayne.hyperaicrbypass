package com.wayne.hyperaicrbypass.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BypassConfigTest {
    @Test
    public void defaultsEnableMasterAndEveryPolicy() {
        BypassConfig config = BypassConfig.defaults();

        assertTrue(config.isMasterEnabled());
        assertTrue(config.isSelectAllMode());
        assertEquals(11, Policy.values().length);
        for (Policy policy : Policy.values()) {
            assertTrue(policy.name(), config.isSelected(policy));
            assertTrue(policy.name(), config.shouldBypass(policy));
        }
    }

    @Test
    public void leavingSelectAllModePreservesChildrenAndEnablesIndividualEditing() {
        BypassConfig allMode = BypassConfig.defaults();
        BypassConfig individualMode = allMode.withSelectAllMode(false);

        assertFalse(individualMode.isSelectAllMode());
        for (Policy policy : Policy.values()) {
            assertTrue(policy.name(), individualMode.isSelected(policy));
        }
        assertEquals(allMode.getConfigRevision() + 1, individualMode.getConfigRevision());

        BypassConfig selectedAgain = individualMode
                .withPolicy(Policy.CHARGING, false)
                .withSelectAllMode(true);
        assertTrue(selectedAgain.isSelectAllMode());
        for (Policy policy : Policy.values()) {
            assertTrue(policy.name(), selectedAgain.isSelected(policy));
        }
    }

    @Test
    public void masterOffPassesThroughWithoutLosingChildSelections() {
        BypassConfig configured = BypassConfig.defaults()
                .withPolicy(Policy.CHARGING, false)
                .withMaster(false);

        for (Policy policy : Policy.values()) {
            assertFalse(policy.name(), configured.shouldBypass(policy));
        }
        assertFalse(configured.isSelected(Policy.CHARGING));
        assertTrue(configured.isSelected(Policy.TEMPERATURE));

        BypassConfig restored = configured.withMaster(true);
        assertFalse(restored.shouldBypass(Policy.CHARGING));
        assertTrue(restored.shouldBypass(Policy.TEMPERATURE));
    }

    @Test
    public void selectAllMutatesChildrenButNeverMaster() {
        BypassConfig masterOff = BypassConfig.defaults().withMaster(false);
        BypassConfig cleared = masterOff.withAllPolicies(false);

        assertFalse(cleared.isMasterEnabled());
        for (Policy policy : Policy.values()) {
            assertFalse(policy.name(), cleared.isSelected(policy));
        }

        BypassConfig selected = cleared.withAllPolicies(true);
        assertFalse(selected.isMasterEnabled());
        for (Policy policy : Policy.values()) {
            assertTrue(policy.name(), selected.isSelected(policy));
            assertFalse(policy.name(), selected.shouldBypass(policy));
        }
    }

    @Test
    public void changingOnePolicyDoesNotChangeAnyOtherPolicy() {
        for (Policy changed : Policy.values()) {
            BypassConfig config = BypassConfig.defaults().withPolicy(changed, false);
            for (Policy observed : Policy.values()) {
                assertEquals(observed.name(), observed != changed, config.isSelected(observed));
            }
        }
    }

    @Test
    public void immutableUpdatesAdvanceConfigRevisionOnlyWhenStateChanges() {
        BypassConfig initial = BypassConfig.defaults();
        BypassConfig unchanged = initial.withPolicy(Policy.POWER, true);
        BypassConfig changed = initial.withPolicy(Policy.POWER, false);

        assertEquals(initial, unchanged);
        assertEquals(initial.getConfigRevision(), unchanged.getConfigRevision());
        assertEquals(initial.getConfigRevision() + 1, changed.getConfigRevision());
        assertEquals(initial.getRescanGeneration(), changed.getRescanGeneration());
    }

    @Test
    public void operatingModesAreMutuallyExclusiveButNormalKeepsSelections() {
        BypassConfig normal = BypassConfig.defaults()
                .withPolicy(Policy.CHARGING, false)
                .withMode(OperatingMode.NORMAL);

        assertEquals(OperatingMode.NORMAL, normal.getMode());
        assertFalse(normal.isMasterEnabled());
        assertFalse(normal.shouldBypass(Policy.TEMPERATURE));
        assertFalse(normal.isSelected(Policy.CHARGING));

        BypassConfig powerSave = normal.withMode(OperatingMode.POWER_SAVE);
        assertEquals(OperatingMode.POWER_SAVE, powerSave.getMode());
        assertFalse(powerSave.isMasterEnabled());
        assertFalse(powerSave.shouldBypass(Policy.TEMPERATURE));

        BypassConfig bypass = powerSave.withMode(OperatingMode.BYPASS);
        assertTrue(bypass.isMasterEnabled());
        assertTrue(bypass.shouldBypass(Policy.TEMPERATURE));
        assertFalse(bypass.isSelected(Policy.CHARGING));
    }

    @Test
    public void powerSaveUsesSelectedBypassesOnlyWhileExternalPowerIsAllowedAndConnected() {
        BypassConfig config = BypassConfig.defaults()
                .withMode(OperatingMode.POWER_SAVE)
                .withPowerException(true);

        assertEquals(OperatingMode.POWER_SAVE, config.effectiveMode(false));
        assertEquals(OperatingMode.BYPASS, config.effectiveMode(true));
        assertTrue(config.shouldPause(false));
        assertFalse(config.shouldPause(true));
        assertFalse(config.shouldBypass(Policy.TEMPERATURE, false));
        assertTrue(config.shouldBypass(Policy.TEMPERATURE, true));
    }

    @Test
    public void preciseProgressCanBeDisabledAndRestoredAtItsLastDecimalScale() {
        BypassConfig hundredths = BypassConfig.defaults()
                .withProgressPrecision(ProgressPrecision.HUNDREDTHS);
        BypassConfig original = hundredths.withPreciseProgress(false);

        assertEquals(ProgressPrecision.ORIGINAL, original.getProgressPrecision());
        assertEquals(ProgressPrecision.HUNDREDTHS, original.getLastNonOriginalPrecision());

        BypassConfig restored = original.withPreciseProgress(true);
        assertEquals(ProgressPrecision.HUNDREDTHS, restored.getProgressPrecision());
        assertEquals(ProgressPrecision.HUNDREDTHS, restored.getLastNonOriginalPrecision());
    }

    @Test
    public void originalCannotReplaceLastNonOriginalPrecision() {
        assertThrows(IllegalArgumentException.class, () -> BypassConfig.defaults()
                .withLastNonOriginalPrecision(ProgressPrecision.ORIGINAL));
    }
}

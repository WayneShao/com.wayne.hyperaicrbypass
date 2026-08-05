package com.wayne.hyperaicrbypass.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
}

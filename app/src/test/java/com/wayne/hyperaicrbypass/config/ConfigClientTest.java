package com.wayne.hyperaicrbypass.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigClientTest {
    @Test
    public void normalNeverBypassesOrPauses() {
        ConfigClient client = new ConfigClient(
                BypassConfig.defaults().withMode(OperatingMode.NORMAL), () -> false
        );

        assertEquals(OperatingMode.NORMAL, client.effectiveMode());
        assertFalse(client.shouldBypass(Policy.TEMPERATURE));
        assertFalse(client.shouldPause());
    }

    @Test
    public void bypassUsesSelectedPoliciesWithoutPausing() {
        ConfigClient client = new ConfigClient(
                BypassConfig.defaults().withPolicy(Policy.CHARGING, false), () -> false
        );

        assertEquals(OperatingMode.BYPASS, client.effectiveMode());
        assertTrue(client.shouldBypass(Policy.TEMPERATURE));
        assertFalse(client.shouldBypass(Policy.CHARGING));
        assertFalse(client.shouldPause());
    }

    @Test
    public void powerSaveTransitionsImmediatelyWithExternalPower() {
        AtomicBoolean connected = new AtomicBoolean(false);
        ConfigClient client = new ConfigClient(
                BypassConfig.defaults()
                        .withMode(OperatingMode.POWER_SAVE)
                        .withPowerException(true),
                connected::get
        );

        assertEquals(OperatingMode.POWER_SAVE, client.effectiveMode());
        assertTrue(client.shouldPause());
        assertFalse(client.shouldBypass(Policy.TEMPERATURE));

        connected.set(true);

        assertEquals(OperatingMode.BYPASS, client.effectiveMode());
        assertFalse(client.shouldPause());
        assertTrue(client.shouldBypass(Policy.TEMPERATURE));
    }

    @Test
    public void progressPrecisionIsIndependentOfRuntimeMode() {
        ConfigClient client = new ConfigClient(
                BypassConfig.defaults()
                        .withMode(OperatingMode.POWER_SAVE)
                        .withProgressPrecision(ProgressPrecision.TENTHS),
                () -> false
        );

        assertEquals(ProgressPrecision.TENTHS, client.progressPrecision());
    }
}

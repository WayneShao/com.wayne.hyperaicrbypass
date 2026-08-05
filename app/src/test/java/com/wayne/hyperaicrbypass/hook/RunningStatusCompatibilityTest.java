package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;

import com.wayne.hyperaicrbypass.config.BypassConfig;
import com.wayne.hyperaicrbypass.config.Policy;

import org.junit.Test;

public final class RunningStatusCompatibilityTest {
    @Test
    public void normalizesOnlyPersistedOverTemperatureStatus() {
        BypassConfig config = BypassConfig.defaults();

        assertEquals(0, RunningStatusCompatibility.normalize(config, "runningStatus", -2));
        assertEquals(4, RunningStatusCompatibility.normalize(config, "runningStatus", 4));
        assertEquals(-2, RunningStatusCompatibility.normalize(config, "otherStatus", -2));
    }

    @Test
    public void temperatureSwitchControlsCompatibilityMapping() {
        BypassConfig temperatureOff = BypassConfig.defaults().withPolicy(Policy.TEMPERATURE, false);
        BypassConfig masterOff = BypassConfig.defaults().withMaster(false);

        assertEquals(-2,
                RunningStatusCompatibility.normalize(temperatureOff, "runningStatus", -2));
        assertEquals(-2,
                RunningStatusCompatibility.normalize(masterOff, "runningStatus", -2));
    }
}

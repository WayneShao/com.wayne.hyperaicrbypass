package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.BatteryManager;

import org.junit.Test;

public class ExternalPowerMonitorTest {
    @Test
    public void usbAcWirelessAndCombinedSourcesAreConnected() {
        assertTrue(ExternalPowerMonitor.isConnectedPowerSource(
                BatteryManager.BATTERY_PLUGGED_USB));
        assertTrue(ExternalPowerMonitor.isConnectedPowerSource(
                BatteryManager.BATTERY_PLUGGED_AC));
        assertTrue(ExternalPowerMonitor.isConnectedPowerSource(
                BatteryManager.BATTERY_PLUGGED_WIRELESS));
        assertTrue(ExternalPowerMonitor.isConnectedPowerSource(
                BatteryManager.BATTERY_PLUGGED_USB | BatteryManager.BATTERY_PLUGGED_AC));
    }

    @Test
    public void unknownAndDisconnectedSourcesAreNotConnected() {
        assertFalse(ExternalPowerMonitor.isConnectedPowerSource(0));
        assertFalse(ExternalPowerMonitor.isConnectedPowerSource(-1));
        assertFalse(ExternalPowerMonitor.isConnectedPowerSource(16));
    }
}

package com.wayne.hyperaicrbypass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.BatteryManager;

import org.junit.Test;

public class SpoofValuesTest {
    @Test
    public void batteryIntentLevelIsForcedToFull() {
        assertEquals(Integer.valueOf(100), SpoofValues.batteryIntentInt(BatteryManager.EXTRA_LEVEL));
        assertEquals(Integer.valueOf(100), SpoofValues.batteryIntentInt(BatteryManager.EXTRA_SCALE));
    }

    @Test
    public void batteryIntentStatusIsForcedToCharging() {
        assertEquals(
                Integer.valueOf(BatteryManager.BATTERY_STATUS_CHARGING),
                SpoofValues.batteryIntentInt(BatteryManager.EXTRA_STATUS)
        );
        assertEquals(
                Integer.valueOf(BatteryManager.BATTERY_PLUGGED_USB),
                SpoofValues.batteryIntentInt(BatteryManager.EXTRA_PLUGGED)
        );
    }

    @Test
    public void batteryIntentTemperatureIsForcedLow() {
        assertEquals(Integer.valueOf(250), SpoofValues.batteryIntentInt("temperature"));
    }

    @Test
    public void lowBatteryFlagsAreForcedFalse() {
        assertFalse(SpoofValues.batteryIntentBoolean("battery_low"));
        assertFalse(SpoofValues.batteryIntentBoolean("is_battery_low"));
        assertTrue(SpoofValues.batteryIntentBoolean("present"));
    }

    @Test
    public void batteryPropertiesOnlySpoofKnownIds() {
        assertEquals(
                Integer.valueOf(100),
                SpoofValues.batteryIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        );
        assertEquals(
                Long.valueOf(6_000_000L),
                SpoofValues.batteryLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        );
        assertNull(SpoofValues.batteryIntProperty(-1));
        assertNull(SpoofValues.batteryLongProperty(-1));
    }
}

package com.wayne.hyperaicrbypass;

import android.os.BatteryManager;

final class SpoofValues {
    static final float COOL_TEMPERATURE_C = 25.0f;
    static final int BATTERY_LEVEL = 100;
    static final int BATTERY_SCALE = 100;
    static final int BATTERY_TENTHS_C = 250;

    private SpoofValues() {
    }

    static Integer batteryIntentInt(String key) {
        if (key == null) {
            return null;
        }
        return switch (key) {
            case BatteryManager.EXTRA_LEVEL -> BATTERY_LEVEL;
            case BatteryManager.EXTRA_SCALE -> BATTERY_SCALE;
            case BatteryManager.EXTRA_STATUS -> BatteryManager.BATTERY_STATUS_CHARGING;
            case BatteryManager.EXTRA_PLUGGED -> BatteryManager.BATTERY_PLUGGED_USB;
            case BatteryManager.EXTRA_HEALTH -> BatteryManager.BATTERY_HEALTH_GOOD;
            case "temperature" -> BATTERY_TENTHS_C;
            default -> null;
        };
    }

    static Boolean batteryIntentBoolean(String key) {
        if (key == null) {
            return null;
        }
        if ("present".equals(key)) {
            return Boolean.TRUE;
        }
        if (key.contains("low")) {
            return Boolean.FALSE;
        }
        return null;
    }

    static Integer batteryIntProperty(int propertyId) {
        if (propertyId == BatteryManager.BATTERY_PROPERTY_CAPACITY) {
            return BATTERY_LEVEL;
        }
        return null;
    }

    static Long batteryLongProperty(int propertyId) {
        if (propertyId == BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) {
            return 6_000_000L;
        }
        if (propertyId == BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) {
            return 0L;
        }
        return null;
    }
}

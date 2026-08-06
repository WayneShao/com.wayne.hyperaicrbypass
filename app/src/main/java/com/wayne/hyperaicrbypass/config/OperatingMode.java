package com.wayne.hyperaicrbypass.config;

public enum OperatingMode {
    NORMAL,
    BYPASS,
    POWER_SAVE;

    public OperatingMode effective(boolean allowWhilePowered, boolean connected) {
        if (this != POWER_SAVE) {
            return this;
        }
        return allowWhilePowered && connected ? BYPASS : POWER_SAVE;
    }

    public static OperatingMode fromStored(String value, boolean legacyMasterEnabled) {
        return value == null
                ? (legacyMasterEnabled ? BYPASS : NORMAL)
                : OperatingMode.valueOf(value);
    }
}

package com.wayne.hyperaicrbypass.hook;

public enum BrowserHookCoverage {
    PENDING,
    AVAILABLE,
    PARTIAL,
    UNAVAILABLE;

    public static BrowserHookCoverage assess(int installed, int expected) {
        if (installed <= 0) {
            return UNAVAILABLE;
        }
        return installed >= expected ? AVAILABLE : PARTIAL;
    }
}

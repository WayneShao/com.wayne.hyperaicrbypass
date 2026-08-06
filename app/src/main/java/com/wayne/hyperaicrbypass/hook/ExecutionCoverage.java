package com.wayne.hyperaicrbypass.hook;

public enum ExecutionCoverage {
    PENDING,
    AVAILABLE,
    UNAVAILABLE;

    public static ExecutionCoverage assess(
            boolean startInstalled,
            boolean needStopInstalled,
            boolean databaseStartGateInstalled,
            boolean uiStartGateInstalled
    ) {
        return startInstalled
                && needStopInstalled
                && databaseStartGateInstalled
                && uiStartGateInstalled
                ? AVAILABLE
                : UNAVAILABLE;
    }
}

package com.wayne.hyperaicrbypass.hook;

import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;

public enum ExecutionCoverage {
    PENDING,
    AVAILABLE,
    UNAVAILABLE;

    public static ExecutionCoverage assess(
            boolean startInstalled,
            boolean needStopInstalled,
            boolean databaseStartGateInstalled,
            boolean optionalUiStartGateInstalled
    ) {
        return startInstalled
                && needStopInstalled
                && databaseStartGateInstalled
                ? AVAILABLE
                : UNAVAILABLE;
    }

    public static boolean shouldAccept(
            DiscoveryKey current,
            DiscoveryKey incoming,
            ExecutionCoverage incomingCoverage
    ) {
        if (current == null) {
            return true;
        }
        if (incomingCoverage != PENDING) {
            return current.equals(incoming);
        }
        return current.equals(incoming) || incoming.isNewerThan(current);
    }
}

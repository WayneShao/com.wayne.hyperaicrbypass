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
        int updateOrder = Long.compare(incoming.lastUpdateTime(), current.lastUpdateTime());
        if (updateOrder != 0) {
            return updateOrder > 0;
        }
        int versionOrder = Long.compare(incoming.versionCode(), current.versionCode());
        if (versionOrder != 0) {
            return versionOrder > 0;
        }
        int schemaOrder = Integer.compare(
                incoming.schemaRevision(), current.schemaRevision()
        );
        if (schemaOrder != 0) {
            return schemaOrder > 0;
        }
        return incoming.rescanGeneration() >= current.rescanGeneration();
    }
}

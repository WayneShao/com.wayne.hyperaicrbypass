package com.wayne.hyperaicrbypass.hook;

import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;

public enum PreciseProgressCoverage {
    PENDING,
    AVAILABLE,
    PARTIAL,
    UNAVAILABLE;

    public static PreciseProgressCoverage assess(int installed, int expected) {
        if (installed <= 0) {
            return UNAVAILABLE;
        }
        return installed >= expected ? AVAILABLE : PARTIAL;
    }

    public static boolean shouldAccept(
            DiscoveryKey currentKey,
            PreciseProgressCoverage currentCoverage,
            int currentCount,
            DiscoveryKey incomingKey,
            PreciseProgressCoverage incomingCoverage,
            int incomingCount
    ) {
        if (incomingKey == null) {
            return false;
        }
        if (currentKey == null) {
            return true;
        }
        if (!currentKey.equals(incomingKey)) {
            if (incomingKey.versionCode() == currentKey.versionCode()
                    && incomingKey.lastUpdateTime() == currentKey.lastUpdateTime()
                    && incomingKey.schemaRevision() == currentKey.schemaRevision()) {
                return incomingKey.rescanGeneration() > currentKey.rescanGeneration();
            }
            return incomingKey.lastUpdateTime() > currentKey.lastUpdateTime();
        }
        if (incomingCount != currentCount) {
            return incomingCount > currentCount;
        }
        return rank(incomingCoverage) >= rank(currentCoverage);
    }

    private static int rank(PreciseProgressCoverage coverage) {
        return switch (coverage) {
            case PENDING -> 0;
            case UNAVAILABLE -> 1;
            case PARTIAL -> 2;
            case AVAILABLE -> 3;
        };
    }
}

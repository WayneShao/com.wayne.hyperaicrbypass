package com.wayne.hyperaicrbypass.hook;

import java.util.Optional;

public final class GlobalProgressHookLogic {
    private GlobalProgressHookLogic() {
    }

    public static int normalizeUiStatus(
            int scope,
            int status,
            int progress,
            boolean initiativeStart,
            boolean initiativePause,
            boolean pausedByHandle
    ) {
        if (scope == 31 && progress < 0 && initiativeStart
                && !initiativePause && !pausedByHandle) {
            return -1;
        }
        if (scope == 31
                && status <= 0
                && initiativeStart
                && !initiativePause
                && !pausedByHandle) {
            return 4;
        }
        return status;
    }

    public static boolean shouldForceNotification(
            boolean migratedReady,
            boolean unmigratedReady,
            int scope
    ) {
        if (scope == 31) {
            return migratedReady || unmigratedReady;
        }
        if (scope == 1) {
            return migratedReady;
        }
        return (migratedReady || unmigratedReady)
                && (scope == 2 || scope == 4 || scope == 8 || scope == 16);
    }

    public static Optional<GlobalProgressSnapshot> displaySnapshot(
            Optional<GlobalProgressSnapshot> payload,
            GlobalProgressSnapshot cached,
            int progress,
            long currentRunStartTime,
            long nowElapsedRealtime,
            boolean explicitTransition
    ) {
        GlobalProgressSnapshot candidate = payload.orElse(cached);
        if (candidate == null
                || candidate.fixedProgress() != progress
                || currentRunStartTime < 0L
                || nowElapsedRealtime < candidate.capturedElapsedRealtime()) {
            return Optional.empty();
        }
        if (candidate.isDisplayCompatible(
                progress, currentRunStartTime, nowElapsedRealtime)) {
            return Optional.of(candidate);
        }
        if (!explicitTransition) {
            return Optional.empty();
        }
        return Optional.of(new GlobalProgressSnapshot(
                candidate.thousandthsPercent(),
                candidate.fixedProgress(),
                candidate.branch(),
                currentRunStartTime,
                candidate.requestGeneration(),
                nowElapsedRealtime
        ));
    }
}

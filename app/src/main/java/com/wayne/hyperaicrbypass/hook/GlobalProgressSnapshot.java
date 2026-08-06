package com.wayne.hyperaicrbypass.hook;

import java.util.Objects;
import java.util.Optional;

public record GlobalProgressSnapshot(
        int thousandthsPercent,
        int fixedProgress,
        GlobalProgressBranch branch,
        long runStartTime,
        long requestGeneration,
        long capturedElapsedRealtime
) {
    static final long MAX_AGE_MILLIS = 360_000L;

    public GlobalProgressSnapshot {
        if (thousandthsPercent < 0
                || thousandthsPercent > 100_000
                || fixedProgress < 0
                || fixedProgress > 100
                || branch == null
                || runStartTime < 0L
                || requestGeneration <= 0L
                || capturedElapsedRealtime < 0L) {
            throw new IllegalArgumentException("Invalid global progress snapshot");
        }
    }

    public static Optional<GlobalProgressSnapshot> restore(
            int thousandthsPercent,
            int fixedProgress,
            String branch,
            long runStartTime,
            long requestGeneration,
            long capturedElapsedRealtime
    ) {
        try {
            return Optional.of(new GlobalProgressSnapshot(
                    thousandthsPercent,
                    fixedProgress,
                    GlobalProgressBranch.valueOf(Objects.requireNonNull(branch)),
                    runStartTime,
                    requestGeneration,
                    capturedElapsedRealtime
            ));
        } catch (IllegalArgumentException | NullPointerException error) {
            return Optional.empty();
        }
    }

    public boolean isCompatible(
            int progress,
            long currentRunStartTime,
            long nowElapsedRealtime
    ) {
        long age = nowElapsedRealtime - capturedElapsedRealtime;
        return fixedProgress == progress
                && runStartTime == currentRunStartTime
                && age >= 0L
                && age <= MAX_AGE_MILLIS;
    }

    public boolean isDisplayCompatible(
            int progress,
            long currentRunStartTime,
            long nowElapsedRealtime
    ) {
        return isCompatible(progress, currentRunStartTime, nowElapsedRealtime);
    }
}

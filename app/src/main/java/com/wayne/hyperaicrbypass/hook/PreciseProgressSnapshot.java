package com.wayne.hyperaicrbypass.hook;

import java.util.Optional;

public record PreciseProgressSnapshot(
        long numerator,
        long denominator,
        int fixedProgress,
        long capturedElapsedRealtime
) {
    static final long MAX_AGE_MILLIS = 360_000L;

    public PreciseProgressSnapshot {
        if (denominator < 0L
                || (denominator == 0L && (numerator != 0L || fixedProgress != 100))
                || fixedProgress < 0
                || fixedProgress > 100
                || capturedElapsedRealtime < 0L) {
            throw new IllegalArgumentException("Invalid precise progress snapshot");
        }
    }

    public static Optional<PreciseProgressSnapshot> create(
            int totalPic,
            int totalVid,
            int faceCount,
            int ocrCount,
            int tagCount,
            int clipPicCount,
            int clipVidCount,
            int petCount,
            int fixedProgress,
            long capturedElapsedRealtime
    ) {
        long denominator = ((long) totalPic * 5L) + (long) totalVid;
        if (denominator <= 0L) {
            return fixedProgress == 100
                    ? restore(0L, 0L, 100, capturedElapsedRealtime)
                    : Optional.empty();
        }
        long numerator = (long) faceCount
                + (long) ocrCount
                + (long) tagCount
                + (long) petCount
                + (long) clipPicCount
                + (long) clipVidCount;
        return restore(numerator, denominator, fixedProgress, capturedElapsedRealtime);
    }

    public static Optional<PreciseProgressSnapshot> restore(
            long numerator,
            long denominator,
            int fixedProgress,
            long capturedElapsedRealtime
    ) {
        try {
            return Optional.of(new PreciseProgressSnapshot(
                    numerator, denominator, fixedProgress, capturedElapsedRealtime
            ));
        } catch (IllegalArgumentException error) {
            return Optional.empty();
        }
    }

    public boolean isDenominatorlessCompletion() {
        return denominator == 0L && numerator == 0L && fixedProgress == 100;
    }

    public boolean isCompatible(int uiProgress, long nowElapsedRealtime) {
        long age = nowElapsedRealtime - capturedElapsedRealtime;
        return fixedProgress == uiProgress && age >= 0L && age <= MAX_AGE_MILLIS;
    }

    public boolean isDisplayCompatible(int uiProgress, long nowElapsedRealtime) {
        return fixedProgress == uiProgress
                && nowElapsedRealtime >= capturedElapsedRealtime;
    }
}

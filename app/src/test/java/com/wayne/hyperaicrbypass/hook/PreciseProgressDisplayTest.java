package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class PreciseProgressDisplayTest {
    @Test
    public void roundsHalfUpToExactlyThreeFractionalDigits() {
        assertEquals("70.254%", PreciseProgressDisplay.format(
                snapshot(7_025_449L, 10_000_000L, 70, 0L)));
        assertEquals("70.255%", PreciseProgressDisplay.format(
                snapshot(7_025_450L, 10_000_000L, 70, 0L)));
    }

    @Test
    public void formatsZeroCompletionAndClampedOverflow() {
        assertEquals("0.000%", PreciseProgressDisplay.format(
                snapshot(0L, 10L, 0, 0L)));
        assertEquals("100.000%", PreciseProgressDisplay.format(
                snapshot(11L, 10L, 100, 0L)));
        assertEquals("100.000%", PreciseProgressDisplay.format(
                PreciseProgressSnapshot.create(
                        0, 0, 0, 0, 0, 0, 0, 0, 100, 0L
                ).orElseThrow()));
        assertEquals("0.000%", PreciseProgressDisplay.format(
                snapshot(-1L, 10L, 0, 0L)));
    }

    @Test
    public void replacesOnlyFirstAsciiPercentageToken() {
        assertEquals("70.254% / 70%", PreciseProgressDisplay.replaceFirstPercentage(
                "70% / 70%", "70.254%"));
        assertEquals("Done 70.254%, wait", PreciseProgressDisplay.replaceFirstPercentage(
                "Done 70.1%, wait", "70.254%"));
        assertEquals("No progress", PreciseProgressDisplay.replaceFirstPercentage(
                "No progress", "70.254%"));
    }

    @Test
    public void rendersOnlyCompatibleGalleryProgress() {
        PreciseProgressSnapshot snapshot = snapshot(
                7_025_449L, 10_000_000L, 70, 1_000L
        );

        assertEquals("Done 70.254%", PreciseProgressDisplay.render(
                "Done 70%", "com.miui.gallery", 70, snapshot, 2_000L));
        assertEquals("Done 70%", PreciseProgressDisplay.render(
                "Done 70%", "com.example.other", 70, snapshot, 2_000L));
        assertEquals("Done 70%", PreciseProgressDisplay.render(
                "Done 70%", "com.miui.gallery", 69, snapshot, 2_000L));
        assertEquals("Done 70%", PreciseProgressDisplay.render(
                "Done 70%", "com.miui.gallery", 70, snapshot, 361_001L));
        assertEquals("Done 70%", PreciseProgressDisplay.render(
                "Done 70%", "com.miui.gallery", 70, null, 2_000L));
    }

    private static PreciseProgressSnapshot snapshot(
            long numerator,
            long denominator,
            int fixedProgress,
            long capturedElapsedRealtime
    ) {
        return PreciseProgressSnapshot.restore(
                numerator, denominator, fixedProgress, capturedElapsedRealtime
        ).orElseThrow();
    }
}

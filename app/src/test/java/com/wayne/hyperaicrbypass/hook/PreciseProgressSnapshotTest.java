package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PreciseProgressSnapshotTest {
    @Test
    public void calculatesNumeratorAndDenominatorFromAllEightInputs() {
        PreciseProgressSnapshot snapshot = PreciseProgressSnapshot.create(
                100_000, 95_999,
                83_754, 83_939, 83_953, 83_173, 0, 83_895,
                70, 1_000L
        ).orElseThrow();

        assertEquals(418_714L, snapshot.numerator());
        assertEquals(595_999L, snapshot.denominator());
        assertEquals(70, snapshot.fixedProgress());
        assertEquals(1_000L, snapshot.capturedElapsedRealtime());
    }

    @Test
    public void usesLongArithmeticBeforeMultiplicationAndAddition() {
        PreciseProgressSnapshot snapshot = PreciseProgressSnapshot.create(
                2_000_000_000, 100_000_000,
                2_000_000_000, 2_000_000_000, 2_000_000_000,
                2_000_000_000, 2_000_000_000, 2_000_000_000,
                50, 0L
        ).orElseThrow();

        assertEquals(12_000_000_000L, snapshot.numerator());
        assertEquals(10_100_000_000L, snapshot.denominator());
    }

    @Test
    public void acceptsDenominatorlessAicrCompletionOnlyForResultHundred() {
        PreciseProgressSnapshot completed = PreciseProgressSnapshot.create(
                0, 0, 0, 0, 0, 0, 0, 0, 100, 10L
        ).orElseThrow();

        assertTrue(completed.isDenominatorlessCompletion());
        assertFalse(PreciseProgressSnapshot.create(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 10L
        ).isPresent());
    }

    @Test
    public void compatibilityRequiresMatchingIntegerAndAgeWithinSixMinutes() {
        PreciseProgressSnapshot snapshot = PreciseProgressSnapshot.restore(
                7, 10, 70, 1_000L
        ).orElseThrow();

        assertTrue(snapshot.isCompatible(70, 1_000L));
        assertTrue(snapshot.isCompatible(70, 361_000L));
        assertFalse(snapshot.isCompatible(70, 361_001L));
        assertFalse(snapshot.isCompatible(69, 2_000L));
        assertFalse(snapshot.isCompatible(70, 999L));
    }

    @Test
    public void restoreRejectsMalformedSnapshots() {
        assertFalse(PreciseProgressSnapshot.restore(1, -1, 10, 0L).isPresent());
        assertFalse(PreciseProgressSnapshot.restore(1, 0, 100, 0L).isPresent());
        assertFalse(PreciseProgressSnapshot.restore(0, 0, 99, 0L).isPresent());
        assertFalse(PreciseProgressSnapshot.restore(1, 1, -1, 0L).isPresent());
        assertFalse(PreciseProgressSnapshot.restore(1, 1, 101, 0L).isPresent());
        assertFalse(PreciseProgressSnapshot.restore(1, 1, 1, -1L).isPresent());
    }
}

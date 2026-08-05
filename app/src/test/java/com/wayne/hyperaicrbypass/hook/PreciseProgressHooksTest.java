package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;
import java.util.Optional;

public final class PreciseProgressHooksTest {
    @Test
    public void parsesEightIntegerArgumentsAndOriginalResult() {
        PreciseProgressSnapshot snapshot = PreciseProgressHookLogic.snapshotFromCalculator(
                new Object[]{100_000, 95_999, 83_754, 83_939, 83_953, 83_173, 0, 83_895},
                70,
                1_000L
        ).orElseThrow();

        assertEquals(418_714L, snapshot.numerator());
        assertEquals(595_999L, snapshot.denominator());
        assertFalse(PreciseProgressHookLogic.snapshotFromCalculator(
                new Object[]{1, 2}, 3, 0L
        ).isPresent());
        assertFalse(PreciseProgressHookLogic.snapshotFromCalculator(
                new Object[]{1, 2, 3, 4, 5, 6, 7, "8"}, 9, 0L
        ).isPresent());
        assertFalse(PreciseProgressHookLogic.snapshotFromCalculator(
                new Object[]{1, 2, 3, 4, 5, 6, 7, 8}, "9", 0L
        ).isPresent());
    }

    @Test
    public void requiredIntegerRejectsMissingAndWrongType() {
        assertEquals(70, PreciseProgressHookLogic.requiredInteger(
                Map.of("analyse_progress", 70), "analyse_progress"
        ).orElseThrow());
        assertFalse(PreciseProgressHookLogic.requiredInteger(
                Map.of(), "analyse_progress"
        ).isPresent());
        assertFalse(PreciseProgressHookLogic.requiredInteger(
                Map.of("analyse_progress", 70L), "analyse_progress"
        ).isPresent());
    }

    @Test
    public void transportsOnlyEnabledCompatibleGallerySnapshots() {
        PreciseProgressSnapshot snapshot = PreciseProgressSnapshot.restore(
                418_714L, 595_999L, 70, 1_000L
        ).orElseThrow();

        assertTrue(PreciseProgressHookLogic.shouldAttach(
                true, 1, 70, snapshot, 2_000L));
        assertFalse(PreciseProgressHookLogic.shouldAttach(
                false, 1, 70, snapshot, 2_000L));
        assertFalse(PreciseProgressHookLogic.shouldAttach(
                true, 2, 70, snapshot, 2_000L));
        assertFalse(PreciseProgressHookLogic.shouldAttach(
                true, 1, null, snapshot, 2_000L));
        assertFalse(PreciseProgressHookLogic.shouldAttach(
                true, 1, 69, snapshot, 2_000L));
        assertFalse(PreciseProgressHookLogic.shouldAttach(
                true, 1, 70, snapshot, 361_001L));
    }

    @Test
    public void displayPrefersNewPayloadAndFallsBackToUiProcessCache() {
        PreciseProgressSnapshot cached = PreciseProgressSnapshot.restore(
                418_000L, 595_999L, 70, 1_000L
        ).orElseThrow();
        PreciseProgressSnapshot payload = PreciseProgressSnapshot.restore(
                418_714L, 595_999L, 70, 2_000L
        ).orElseThrow();

        assertEquals(payload, PreciseProgressHookLogic.displaySnapshot(
                Optional.of(payload), cached
        ).orElseThrow());
        assertEquals(cached, PreciseProgressHookLogic.displaySnapshot(
                Optional.empty(), cached
        ).orElseThrow());
        assertFalse(PreciseProgressHookLogic.displaySnapshot(
                Optional.empty(), null
        ).isPresent());
    }

    @Test
    public void forcesExistingNotificationOnlyForGalleryWithAPreciseSnapshot() {
        PreciseProgressSnapshot payload = PreciseProgressSnapshot.restore(
                418_714L, 595_999L, 70, 2_000L
        ).orElseThrow();

        assertTrue(PreciseProgressHookLogic.shouldForceUiNotification(1, payload));
        assertFalse(PreciseProgressHookLogic.shouldForceUiNotification(2, payload));
        assertFalse(PreciseProgressHookLogic.shouldForceUiNotification(1, null));
    }
}

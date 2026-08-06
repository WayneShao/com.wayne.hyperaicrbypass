package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Optional;

public final class GlobalProgressSnapshotTest {
    @Test
    public void keepsTransportStrictButAllowsStableSameRunDisplay() {
        GlobalProgressSnapshot snapshot = snapshot(12_345L, 8_000L);

        assertTrue(snapshot.isCompatible(85, 12_345L, 9_000L));
        assertFalse(snapshot.isCompatible(85, 12_345L, 368_001L));
        assertTrue(snapshot.isDisplayCompatible(85, 12_345L, 368_001L));
        assertFalse(snapshot.isDisplayCompatible(84, 12_345L, 9_000L));
        assertFalse(snapshot.isDisplayCompatible(85, 12_346L, 9_000L));
        assertFalse(snapshot.isDisplayCompatible(85, 12_345L, 7_999L));
    }

    @Test
    public void explicitTransitionRebindsOnceButArbitraryCrossRunReuseFails() {
        GlobalProgressSnapshot previous = snapshot(12_345L, 8_000L);

        GlobalProgressSnapshot rebound = GlobalProgressHookLogic.displaySnapshot(
                Optional.empty(), previous, 85, 12_346L, 9_000L, true
        ).orElseThrow();

        assertEquals(12_346L, rebound.runStartTime());
        assertEquals(9_000L, rebound.capturedElapsedRealtime());
        assertEquals(previous.thousandthsPercent(), rebound.thousandthsPercent());
        assertTrue(GlobalProgressHookLogic.displaySnapshot(
                Optional.empty(), rebound, 85, 12_346L, 9_001L, false
        ).isPresent());
        assertFalse(GlobalProgressHookLogic.displaySnapshot(
                Optional.empty(), rebound, 85, 12_347L, 9_002L, false
        ).isPresent());
        assertFalse(GlobalProgressHookLogic.displaySnapshot(
                Optional.empty(), previous, 84, 12_346L, 9_000L, true
        ).isPresent());
        assertFalse(GlobalProgressHookLogic.displaySnapshot(
                Optional.empty(), null, 85, 12_346L, 9_000L, true
        ).isPresent());
    }

    private static GlobalProgressSnapshot snapshot(long runStart, long captured) {
        return new GlobalProgressSnapshot(
                85_317, 85, GlobalProgressBranch.MIGRATED_DIRECT_AI,
                runStart, 7L, captured
        );
    }
}

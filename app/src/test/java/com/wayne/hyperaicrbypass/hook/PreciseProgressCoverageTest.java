package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;

import org.junit.Test;

public final class PreciseProgressCoverageTest {
    @Test
    public void assessesCompletePartialAndUnavailableChains() {
        assertEquals(PreciseProgressCoverage.UNAVAILABLE,
                PreciseProgressCoverage.assess(0, 4));
        assertEquals(PreciseProgressCoverage.PARTIAL,
                PreciseProgressCoverage.assess(2, 4));
        assertEquals(PreciseProgressCoverage.AVAILABLE,
                PreciseProgressCoverage.assess(4, 4));
    }

    @Test
    public void rejectsLateKeysAndMergesSameKeyWithoutDowngrading() {
        DiscoveryKey current = new DiscoveryKey(4006, 200, 3, 2);
        DiscoveryKey staleGeneration = new DiscoveryKey(4006, 200, 3, 1);
        DiscoveryKey newerGeneration = new DiscoveryKey(4006, 200, 3, 3);

        assertFalse(PreciseProgressCoverage.shouldAccept(
                current, PreciseProgressCoverage.AVAILABLE, 4,
                staleGeneration, PreciseProgressCoverage.AVAILABLE, 4));
        assertTrue(PreciseProgressCoverage.shouldAccept(
                current, PreciseProgressCoverage.AVAILABLE, 4,
                newerGeneration, PreciseProgressCoverage.PENDING, 0));
        assertFalse(PreciseProgressCoverage.shouldAccept(
                current, PreciseProgressCoverage.AVAILABLE, 4,
                current, PreciseProgressCoverage.PARTIAL, 2));
    }
}

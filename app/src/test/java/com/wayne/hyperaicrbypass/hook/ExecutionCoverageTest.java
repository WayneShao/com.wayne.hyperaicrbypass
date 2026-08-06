package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;

public class ExecutionCoverageTest {
    @Test
    public void executionSafetyRequiresBothStatusHooksAndDatabaseStartGate() {
        assertEquals(ExecutionCoverage.AVAILABLE,
                ExecutionCoverage.assess(true, true, true, true));
        assertEquals(ExecutionCoverage.UNAVAILABLE,
                ExecutionCoverage.assess(false, true, true, true));
        assertEquals(ExecutionCoverage.UNAVAILABLE,
                ExecutionCoverage.assess(true, false, true, true));
        assertEquals(ExecutionCoverage.UNAVAILABLE,
                ExecutionCoverage.assess(true, true, false, true));
        assertEquals(ExecutionCoverage.AVAILABLE,
                ExecutionCoverage.assess(true, true, true, false));
    }

    @Test
    public void stalePendingAndFinalReportsCannotReplaceTheCurrentDiscoveryKey() {
        DiscoveryKey current = new DiscoveryKey(20L, 200L, 3, 4L);

        assertFalse(ExecutionCoverage.shouldAccept(
                current, new DiscoveryKey(20L, 200L, 3, 3L),
                ExecutionCoverage.PENDING));
        assertFalse(ExecutionCoverage.shouldAccept(
                current, new DiscoveryKey(19L, 100L, 3, 99L),
                ExecutionCoverage.PENDING));
        assertFalse(ExecutionCoverage.shouldAccept(
                current, new DiscoveryKey(20L, 200L, 3, 5L),
                ExecutionCoverage.AVAILABLE));
        assertTrue(ExecutionCoverage.shouldAccept(
                current, current, ExecutionCoverage.AVAILABLE));
        assertTrue(ExecutionCoverage.shouldAccept(
                current, new DiscoveryKey(20L, 200L, 3, 5L),
                ExecutionCoverage.PENDING));
        assertTrue(ExecutionCoverage.shouldAccept(
                current, new DiscoveryKey(21L, 300L, 3, 0L),
                ExecutionCoverage.PENDING));
    }
}

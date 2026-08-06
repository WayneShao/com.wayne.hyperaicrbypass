package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExecutionCoverageTest {
    @Test
    public void allFourCriticalBranchesAreRequiredForAvailability() {
        assertEquals(ExecutionCoverage.AVAILABLE,
                ExecutionCoverage.assess(true, true, true, true));
        assertEquals(ExecutionCoverage.UNAVAILABLE,
                ExecutionCoverage.assess(false, true, true, true));
        assertEquals(ExecutionCoverage.UNAVAILABLE,
                ExecutionCoverage.assess(true, false, true, true));
        assertEquals(ExecutionCoverage.UNAVAILABLE,
                ExecutionCoverage.assess(true, true, false, true));
        assertEquals(ExecutionCoverage.UNAVAILABLE,
                ExecutionCoverage.assess(true, true, true, false));
    }
}

package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RescanGenerationGateTest {
    @Test
    public void onlyStrictlyNewGenerationsTriggerDiscovery() {
        RescanGenerationGate gate = new RescanGenerationGate(4);

        assertFalse(gate.tryAdvance(4));
        assertFalse(gate.tryAdvance(3));
        assertTrue(gate.tryAdvance(5));
        assertFalse(gate.tryAdvance(5));
        assertTrue(gate.tryAdvance(7));
    }
}

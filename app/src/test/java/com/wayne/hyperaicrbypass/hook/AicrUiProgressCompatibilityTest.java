package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AicrUiProgressCompatibilityTest {
    @Test
    public void disabledPolicyPreservesProviderResponse() {
        AicrUiProgressCompatibility.Result result =
                AicrUiProgressCompatibility.normalize(false, 27, false, -1, false, -1);

        assertFalse(result.changed());
        assertFalse(result.hasSupport());
        assertFalse(result.hasInProgress());
        assertEquals(-1, result.support());
        assertEquals(-1, result.inProgress());
    }

    @Test
    public void enabledPolicyPublishesSupportedRunningState() {
        AicrUiProgressCompatibility.Result result =
                AicrUiProgressCompatibility.normalize(true, 27, false, -1, false, -1);

        assertTrue(result.changed());
        assertTrue(result.hasSupport());
        assertTrue(result.hasInProgress());
        assertEquals(1, result.support());
        assertEquals(2, result.inProgress());
    }

    @Test
    public void completedProgressPublishesIdleState() {
        AicrUiProgressCompatibility.Result result =
                AicrUiProgressCompatibility.normalize(true, 100, true, 0, true, 2);

        assertTrue(result.changed());
        assertEquals(1, result.support());
        assertEquals(0, result.inProgress());
    }
}

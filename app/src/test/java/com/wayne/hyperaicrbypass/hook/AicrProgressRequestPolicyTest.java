package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AicrProgressRequestPolicyTest {
    @Test
    public void forcesLiveForRegisteredGalleryAndGlobalUiProgress() {
        assertTrue(AicrProgressRequestPolicy.shouldForceLive(
                "method_algo_get_progress", 1, true, true));
        assertTrue(AicrProgressRequestPolicy.shouldForceLive(
                "method_algo_get_progress", 31, true, true));
    }

    @Test
    public void leavesNonUiUncachedAndUnrelatedRequestsAlone() {
        assertFalse(AicrProgressRequestPolicy.shouldForceLive(
                "method_algo_get_progress", 1, false, true));
        assertFalse(AicrProgressRequestPolicy.shouldForceLive(
                "method_algo_get_progress", 31, true, false));
        assertFalse(AicrProgressRequestPolicy.shouldForceLive(
                "method_algo_get_progress", 2, true, true));
        assertFalse(AicrProgressRequestPolicy.shouldForceLive(
                "method_refresh_progress", 1, true, true));
        assertFalse(AicrProgressRequestPolicy.shouldForceLive(
                null, 1, true, true));
    }

    @Test
    public void discardsUiCacheForEveryPreciseFirstFrameRequest() {
        assertTrue(AicrProgressRequestPolicy.shouldDiscardUiCache(
                "method_algo_get_progress", 1));
        assertTrue(AicrProgressRequestPolicy.shouldDiscardUiCache(
                "method_algo_get_progress", 31));
        assertFalse(AicrProgressRequestPolicy.shouldDiscardUiCache(
                "method_refresh_progress", 31));
        assertFalse(AicrProgressRequestPolicy.shouldDiscardUiCache(
                "method_algo_get_progress", 2));
    }

    @Test
    public void originalPrecisionDisablesLiveRequestsAndCacheDiscard() {
        assertFalse(AicrProgressRequestPolicy.shouldForceLive(
                false, "method_algo_get_progress", 31, true, true));
        assertFalse(AicrProgressRequestPolicy.shouldDiscardUiCache(
                false, "method_algo_get_progress", 31));
        assertTrue(AicrProgressRequestPolicy.shouldForceLive(
                true, "method_algo_get_progress", 31, true, true));
        assertTrue(AicrProgressRequestPolicy.shouldDiscardUiCache(
                true, "method_algo_get_progress", 31));
    }
}

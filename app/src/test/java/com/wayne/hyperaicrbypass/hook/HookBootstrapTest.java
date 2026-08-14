package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wayne.hyperaicrbypass.config.Policy;

import org.junit.Test;

import java.util.Map;

public final class HookBootstrapTest {
    @Test
    public void emptySemanticCoverageStillIncludesProviderFallback() {
        Map<Policy, Integer> coverage =
                HookBootstrap.coverageWithProviderFallback(Map.of(), true);

        assertEquals(Integer.valueOf(1), coverage.get(Policy.AI_UI_CAPABILITY));
    }

    @Test
    public void onlyCompleteCoreFailureRequestsTheGlobalFailureToast() {
        assertFalse(HookBootstrap.shouldShowTotalFailure(AicrProcessRole.MAIN, 0));
        assertFalse(HookBootstrap.shouldShowTotalFailure(AicrProcessRole.SEARCH_UI, 0));
        assertFalse(HookBootstrap.shouldShowTotalFailure(AicrProcessRole.SEARCH_DATA, 1));
        assertTrue(HookBootstrap.shouldShowTotalFailure(AicrProcessRole.SEARCH_DATA, 0));
    }
}

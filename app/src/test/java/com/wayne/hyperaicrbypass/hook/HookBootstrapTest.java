package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;

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
}

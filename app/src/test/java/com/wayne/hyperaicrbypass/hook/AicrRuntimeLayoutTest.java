package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AicrRuntimeLayoutTest {
    @Test
    public void detectsReadableAndCompactV4LayoutsFromRuntimeMarkers() {
        assertEquals(AicrRuntimeLayout.V4_READABLE,
                AicrRuntimeLayout.detect(AicrVersionBranch.V4, true, false));
        assertEquals(AicrRuntimeLayout.V4_COMPACT,
                AicrRuntimeLayout.detect(AicrVersionBranch.V4, false, true));
        assertEquals(AicrRuntimeLayout.UNKNOWN,
                AicrRuntimeLayout.detect(AicrVersionBranch.V4, false, false));
    }

    @Test
    public void compactLayoutAcceptsOnlyAicrDefaultPackageOrOwnedClasses() {
        assertTrue(SemanticHooks.isExpectedOwner(AicrRuntimeLayout.V4_COMPACT, "qz7"));
        assertTrue(SemanticHooks.isExpectedOwner(
                AicrRuntimeLayout.V4_COMPACT,
                "com.xiaomi.aicr.aisearch.progress.AISearchProgressActivity"));
        assertFalse(SemanticHooks.isExpectedOwner(
                AicrRuntimeLayout.V4_COMPACT, "com.vendor.analytics.Helper"));
    }
}

package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GalleryAicrTraceFilterTest {
    @Test
    public void acceptsOnlyAicrProgressControlCalls() {
        assertTrue(GalleryAicrTraceFilter.shouldTrace(
                "com.xiaomi.aicr.ui.provider", "method_algo_get_progress"));
        assertTrue(GalleryAicrTraceFilter.shouldTrace(
                "provider.SearchDataBaseProvider", "method_algo_get_progress"));
        assertTrue(GalleryAicrTraceFilter.shouldTrace(
                "com.xiaomi.aicr.ui.provider", "method_change_algo_state"));
        assertTrue(GalleryAicrTraceFilter.shouldTrace(
                "com.xiaomi.aicr.ui.provider", "method_release_scope_ui"));

        assertFalse(GalleryAicrTraceFilter.shouldTrace(
                "com.xiaomi.aicr.provider.NLSCapabilityProvider", "query"));
        assertFalse(GalleryAicrTraceFilter.shouldTrace(
                "com.xiaomi.aicr.ui.provider", "search"));
        assertFalse(GalleryAicrTraceFilter.shouldTrace(
                "media", "method_algo_get_progress"));

        assertTrue(GalleryAicrTraceFilter.shouldTraceMethod("method_algo_get_progress"));
        assertTrue(GalleryAicrTraceFilter.shouldTraceMethod("get_progress"));
        assertTrue(GalleryAicrTraceFilter.shouldTraceMethod("refresh_ui_progress"));
        assertFalse(GalleryAicrTraceFilter.shouldTraceMethod("method_updatePeopleInfo"));
    }

    @Test
    public void allowsOnlyNonContentProgressFields() {
        assertTrue(GalleryAicrTraceFilter.shouldLogField("scope"));
        assertTrue(GalleryAicrTraceFilter.shouldLogField("analyse_status"));
        assertTrue(GalleryAicrTraceFilter.shouldLogField("initiative_start"));
        assertTrue(GalleryAicrTraceFilter.shouldLogField("progress"));
        assertTrue(GalleryAicrTraceFilter.shouldLogField("in_progress"));
        assertTrue(GalleryAicrTraceFilter.shouldLogField("is_support_ai_search_progress"));

        assertFalse(GalleryAicrTraceFilter.shouldLogField("query"));
        assertFalse(GalleryAicrTraceFilter.shouldLogField("path"));
        assertFalse(GalleryAicrTraceFilter.shouldLogField("photo"));
    }
}

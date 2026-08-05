package com.wayne.hyperaicrbypass.hook;

import java.util.Set;

public final class GalleryAicrTraceFilter {
    private static final Set<String> AUTHORITIES = Set.of(
            "com.xiaomi.aicr.ui.provider",
            "provider.SearchDataBaseProvider"
    );
    private static final Set<String> METHODS = Set.of(
            "method_algo_get_progress",
            "get_progress",
            "method_change_algo_state",
            "method_release_scope_ui",
            "refresh_ui_progress",
            "method_update_ui_scopes"
    );
    private static final Set<String> FIELDS = Set.of(
            "scope",
            "register_ui_listener",
            "use_cache",
            "only_runState",
            "analyse_progress",
            "analyse_status",
            "global_analyse_progress",
            "initiative_start",
            "initiative_pause",
            "status_change",
            "force_refresh",
            "ui_scopes",
            "has_global_ui_scope",
            "is_run_algo",
            "progress",
            "in_progress",
            "is_support_ai_search_progress",
            "Status"
    );

    private GalleryAicrTraceFilter() {
    }

    public static boolean shouldTrace(String authority, String method) {
        return AUTHORITIES.contains(authority) && METHODS.contains(method);
    }

    public static boolean shouldTraceMethod(String method) {
        return METHODS.contains(method);
    }

    public static boolean shouldLogField(String field) {
        return FIELDS.contains(field);
    }
}

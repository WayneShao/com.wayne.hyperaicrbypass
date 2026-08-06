package com.wayne.hyperaicrbypass.hook;

public final class AicrProgressRequestPolicy {
    private static final String GET_PROGRESS = "method_algo_get_progress";

    private AicrProgressRequestPolicy() {
    }

    public static boolean shouldForceLive(
            String method,
            int scope,
            boolean registerUiListener,
            boolean useCache
    ) {
        return shouldForceLive(true, method, scope, registerUiListener, useCache);
    }

    public static boolean shouldForceLive(
            boolean preciseEnabled,
            String method,
            int scope,
            boolean registerUiListener,
            boolean useCache
    ) {
        return preciseEnabled
                && GET_PROGRESS.equals(method)
                && (scope == 1 || scope == 31)
                && registerUiListener
                && useCache;
    }

    public static boolean shouldDiscardUiCache(String method, int scope) {
        return shouldDiscardUiCache(true, method, scope);
    }

    public static boolean shouldDiscardUiCache(
            boolean preciseEnabled,
            String method,
            int scope
    ) {
        return preciseEnabled
                && GET_PROGRESS.equals(method)
                && (scope == 1 || scope == 31);
    }
}

package com.wayne.hyperaicrbypass.hook;

public enum AicrRuntimeLayout {
    V3_OBFUSCATED,
    V4_READABLE,
    V4_COMPACT,
    UNKNOWN;

    private static final String V4_READABLE_STATUS =
            "com.xiaomi.aicr.searchpro.monitor.RunningStatus";
    private static final String V4_COMPACT_MARKER =
            "com.hyperos.ai.aisearch.searchpro.monitor.RunLevel";

    public static AicrRuntimeLayout detect(
            AicrVersionBranch branch,
            boolean readableStatusPresent,
            boolean compactMarkerPresent
    ) {
        return switch (branch) {
            case V3 -> V3_OBFUSCATED;
            case V4 -> readableStatusPresent
                    ? V4_READABLE
                    : compactMarkerPresent ? V4_COMPACT : UNKNOWN;
            case UNKNOWN -> UNKNOWN;
        };
    }

    public static AicrRuntimeLayout detect(
            AicrVersionBranch branch,
            ClassLoader classLoader
    ) {
        return detect(
                branch,
                isPresent(V4_READABLE_STATUS, classLoader),
                isPresent(V4_COMPACT_MARKER, classLoader)
        );
    }

    public AicrVersionBranch branch() {
        return switch (this) {
            case V3_OBFUSCATED -> AicrVersionBranch.V3;
            case V4_READABLE, V4_COMPACT -> AicrVersionBranch.V4;
            case UNKNOWN -> AicrVersionBranch.UNKNOWN;
        };
    }

    private static boolean isPresent(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}

package com.wayne.hyperaicrbypass.hook;

import java.util.List;

public final class CopyWebsiteBrowserHookCatalog {
    public enum Kind {
        RETURN_INTENT,
        OPEN_URL
    }

    public record Spec(
            Kind kind,
            String returnType,
            List<String> parameterTypes,
            String anchor
    ) {
    }

    private static final List<Spec> V3 = List.of(
            new Spec(Kind.RETURN_INTENT, "android.content.Intent",
                    List.of("java.lang.String"), "clipboard_open"),
            new Spec(Kind.OPEN_URL, "void",
                    List.of("com.xiaomi.aicr.copydirect.IntentActivity", "java.lang.String"),
                    "jumpToMiBrowser: ///////////////////")
    );
    private static final List<Spec> V4 = List.of(
            new Spec(Kind.OPEN_URL, "void",
                    List.of("android.content.Context", "java.lang.String"), "clipboard_open")
    );

    private CopyWebsiteBrowserHookCatalog() {
    }

    public static List<Spec> forBranch(AicrVersionBranch branch) {
        return branch == AicrVersionBranch.V3 ? V3 : V4;
    }

    public static boolean isExpectedOwner(AicrVersionBranch branch, String className) {
        if (className == null) {
            return false;
        }
        return switch (branch) {
            case V3 -> !className.contains(".");
            case V4 -> className.startsWith("com.xiaomi.aicr.copydirect.");
            case UNKNOWN -> !className.contains(".")
                    || className.startsWith("com.xiaomi.aicr.copydirect.");
        };
    }
}

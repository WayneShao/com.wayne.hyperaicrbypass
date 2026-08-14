package com.wayne.hyperaicrbypass.hook;

import java.util.List;
import java.util.Set;

public final class GlobalProgressHookCatalog {
    private static final String PROGRESS_MONITOR =
            "com.xiaomi.aicr.searchpro.monitor.ProgressMonitor";
    private static final String GALLERY_MONITOR =
            "com.xiaomi.aicr.searchpro.monitor.GalleryProgressMonitor";
    private static final String RUNNING_STATUS =
            "com.xiaomi.aicr.searchpro.monitor.RunningStatus";
    private static final String SETTING_ACTIVITY =
            "com.xiaomi.aicr.aisearch.AiSearchSettingActivity";
    private static final String FUNCTION3 = "kotlin.jvm.functions.Function3";

    private static final List<Point> POINTS = List.of(
            new Point(
                    "index", PROGRESS_MONITOR, "getIndexProgress",
                    "android.os.Bundle", List.of("int", "boolean", FUNCTION3),
                    false,
                    List.of("analyse_progress", "analyse_status")
            ),
            new Point(
                    "migrated", PROGRESS_MONITOR, "getMigratedProgress",
                    "int", List.of("int", "boolean", FUNCTION3),
                    false,
                    List.of("enter getMigratedProgress")
            ),
            new Point(
                    "unmigrated", PROGRESS_MONITOR, "getUnMigratedProgress",
                    "int", List.of("int", "boolean", FUNCTION3),
                    false,
                    List.of("enter getUnMigratedProgress")
            ),
            new Point(
                    "local-scope", PROGRESS_MONITOR, "calculateScopeProgress",
                    "int", List.of("int", "boolean", "boolean", "boolean"),
                    false,
                    List.of("scope 31 progress calculate begin")
            ),
            new Point(
                    "local-calculator", PROGRESS_MONITOR, "calculateProgress",
                    "float", List.of("int", "int", "int"),
                    false,
                    List.of("oriCount = ", ", invertedCount = ")
            ),
            new Point(
                    "gallery-boundary", GALLERY_MONITOR, "getGalleryProgress",
                    "int", List.of("boolean", FUNCTION3),
                    false,
                    List.of("getGalleryProgress progress:")
            ),
            new Point(
                    "gallery-calculator", GALLERY_MONITOR, "calculateProgress",
                    "int", List.of("int", "int", "int", "int", "int", "int",
                            "int", "int"),
                    false,
                    List.of("total:0, return 100", "progress = ")
            ),
            new Point(
                    "gallery-postprocess", GALLERY_MONITOR,
                    "calculateProgressOnMigrate", "int",
                    List.of("float", "int", "int", "int", "int"),
                    false,
                    List.of("mediaCountCurr:", "  return 100")
            ),
            new Point(
                    "notification", RUNNING_STATUS, "sendProgressToActivity", "void",
                    List.of("int", "boolean"),
                    false,
                    List.of("enter sendProgressToActivity scopes")
            ),
            new Point(
                    "outgoing-bridge", PROGRESS_MONITOR,
                    "updateScopeUIProgressInfo", "void",
                    List.of("int", "android.os.Bundle"), false, List.of()
            ),
            new Point(
                    "setting-display", SETTING_ACTIVITY,
                    "refreshAISearchStatus", "void",
                    List.of("android.os.Bundle"),
                    false,
                    List.of("analyse_status", "analyse_progress", "download_paused")
            )
    );

    private static final List<Point> V3_POINTS = List.of(
            new Point("index", "ok5", "g", "android.os.Bundle",
                    List.of("int", "boolean", "oa6"), false,
                    List.of("enter getMigratedProgress")),
            new Point("local-scope", "ok5", "b", "int",
                    List.of("int", "boolean", "boolean", "boolean"), false,
                    List.of("scope 31 progress calculate begin")),
            new Point("local-calculator", "ok5", "a", "float",
                    List.of("int", "int", "int"), true,
                    List.of("oriCount = ", ", invertedCount = ")),
            new Point("gallery-boundary", "il2", "d", "int",
                    List.of("boolean", FUNCTION3), true,
                    List.of("getGalleryProgress progress:")),
            new Point("gallery-calculator", "il2", "a", "int",
                    List.of("int", "int", "int", "int", "int", "int"), true,
                    List.of("total:0, return 100", "progress = ")),
            new Point("notification", "u16", "H", "void",
                    List.of("int", "boolean"), false,
                    List.of("enter sendProgressToActivity scopes")),
            new Point("outgoing-bridge", "ok5", "s", "void",
                    List.of("int", "android.os.Bundle"), true, List.of()),
            new Point("setting-display",
                    "com.xiaomi.aicr.aisearch.AiSearchSettingActivity", "s", "void",
                    List.of("com.xiaomi.aicr.aisearch.AiSearchSettingActivity",
                            "android.os.Bundle"), true,
                    List.of("analyse_status", "download_paused"))
    );
    private static final List<Point> V4_COMPACT_POINTS = List.of(
            new Point("index", "ac7", "g", "android.os.Bundle",
                    List.of("int", "boolean", "rb8"), false,
                    List.of("ProgressMonitor.getMigratedProgress", "analyse_progress")),
            new Point("local-scope", "ac7", "b", "int",
                    List.of("int", "boolean", "boolean", "boolean"), false,
                    List.of("scope 31 progress calculate begin")),
            new Point("local-calculator", "ac7", "a", "float",
                    List.of("int", "int", "int"), true,
                    List.of("ProgressMonitor.calculateProgress", "oriCount = ")),
            new Point("gallery-boundary", "ij3", "d", "int",
                    List.of("boolean", FUNCTION3), true,
                    List.of("GalleryProgressMonitor.getGalleryProgress",
                            "getGalleryProgress progress:")),
            new Point("gallery-calculator", "ij3", "a", "int",
                    List.of("int", "int", "int", "int", "int", "int"), true,
                    List.of("GalleryProgressMonitor.calculateProgress",
                            "total:0, return 100", "progress = ")),
            new Point("notification", "qz7", "J", "void",
                    List.of("int", "boolean"), false,
                    List.of("RunningStatus.sendProgressToActivity",
                            "enter sendProgressToActivity scopes")),
            new Point("outgoing-bridge", "ac7", "s", "void",
                    List.of("int", "android.os.Bundle"), true, List.of()),
            new Point("setting-display", SETTING_ACTIVITY, "l", "void",
                    List.of(SETTING_ACTIVITY, "android.os.Bundle"), true,
                    List.of("analyse_status", "analyse_progress", "download_paused"))
    );

    private GlobalProgressHookCatalog() {
    }

    public static List<Point> points() {
        return POINTS;
    }

    public static List<Point> points(AicrVersionBranch branch) {
        return branch == AicrVersionBranch.V3 ? V3_POINTS : POINTS;
    }

    public static List<Point> points(AicrRuntimeLayout layout) {
        return switch (layout) {
            case V3_OBFUSCATED -> V3_POINTS;
            case V4_READABLE -> POINTS;
            case V4_COMPACT -> V4_COMPACT_POINTS;
            case UNKNOWN -> List.of();
        };
    }

    static boolean usesAssignableFunction3(AicrRuntimeLayout layout, Point point) {
        return layout == AicrRuntimeLayout.V4_COMPACT
                && point.id().equals("index");
    }

    public static Set<String> requiredPointIds(GlobalProgressBranch branch) {
        return switch (branch) {
            case MIGRATED_DIRECT_AI -> Set.of(
                    "index", "migrated", "local-scope", "local-calculator",
                    "gallery-boundary", "gallery-calculator",
                    "notification", "outgoing-bridge", "setting-display"
            );
            case MIGRATED_POSTPROCESSED -> Set.of(
                    "index", "migrated", "local-scope", "local-calculator",
                    "gallery-boundary", "gallery-calculator",
                    "gallery-postprocess", "notification",
                    "outgoing-bridge", "setting-display"
            );
            case UNMIGRATED_LOCAL -> Set.of(
                    "index", "unmigrated", "local-scope", "local-calculator",
                    "notification", "outgoing-bridge", "setting-display"
            );
        };
    }

    public static Set<String> requiredPointIds(
            AicrVersionBranch version,
            GlobalProgressBranch branch
    ) {
        if (version != AicrVersionBranch.V3) {
            return requiredPointIds(branch);
        }
        return Set.of(
                "index", "local-scope", "local-calculator", "gallery-boundary",
                "gallery-calculator", "notification", "outgoing-bridge",
                "setting-display"
        );
    }

    public static Set<String> requiredPointIds(
            AicrRuntimeLayout layout,
            GlobalProgressBranch branch
    ) {
        if (layout == AicrRuntimeLayout.V4_READABLE) {
            return requiredPointIds(branch);
        }
        if (layout == AicrRuntimeLayout.V3_OBFUSCATED
                || layout == AicrRuntimeLayout.V4_COMPACT) {
            return Set.of(
                    "index", "local-scope", "local-calculator", "gallery-boundary",
                    "gallery-calculator", "notification", "outgoing-bridge",
                    "setting-display"
            );
        }
        return Set.of();
    }

    public record Point(
            String id,
            String className,
            String methodName,
            String returnType,
            List<String> parameterTypes,
            boolean isStatic,
            List<String> requiredAnchors
    ) {
        public String packageName() {
            int separator = className.lastIndexOf('.');
            return separator < 0 ? "" : className.substring(0, separator);
        }
    }
}

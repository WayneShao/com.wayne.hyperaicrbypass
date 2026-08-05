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
                    List.of("analyse_progress", "analyse_status")
            ),
            new Point(
                    "migrated", PROGRESS_MONITOR, "getMigratedProgress",
                    "int", List.of("int", "boolean", FUNCTION3),
                    List.of("enter getMigratedProgress")
            ),
            new Point(
                    "unmigrated", PROGRESS_MONITOR, "getUnMigratedProgress",
                    "int", List.of("int", "boolean", FUNCTION3),
                    List.of("enter getUnMigratedProgress")
            ),
            new Point(
                    "local-scope", PROGRESS_MONITOR, "calculateScopeProgress",
                    "int", List.of("int", "boolean", "boolean", "boolean"),
                    List.of("scope 31 progress calculate begin")
            ),
            new Point(
                    "local-calculator", PROGRESS_MONITOR, "calculateProgress",
                    "float", List.of("int", "int", "int"),
                    List.of("oriCount = ", ", invertedCount = ")
            ),
            new Point(
                    "gallery-boundary", GALLERY_MONITOR, "getGalleryProgress",
                    "int", List.of("boolean", FUNCTION3),
                    List.of("getGalleryProgress progress:")
            ),
            new Point(
                    "gallery-calculator", GALLERY_MONITOR, "calculateProgress",
                    "int", List.of("int", "int", "int", "int", "int", "int",
                            "int", "int"),
                    List.of("total:0, return 100", "progress = ")
            ),
            new Point(
                    "gallery-postprocess", GALLERY_MONITOR,
                    "calculateProgressOnMigrate", "int",
                    List.of("float", "int", "int", "int", "int"),
                    List.of("mediaCountCurr:", "  return 100")
            ),
            new Point(
                    "notification", RUNNING_STATUS, "sendProgressToActivity", "void",
                    List.of("int", "boolean"),
                    List.of("enter sendProgressToActivity scopes")
            ),
            new Point(
                    "outgoing-bridge", PROGRESS_MONITOR,
                    "updateScopeUIProgressInfo", "void",
                    List.of("int", "android.os.Bundle"), List.of()
            ),
            new Point(
                    "setting-display", SETTING_ACTIVITY,
                    "refreshAISearchStatus", "void",
                    List.of("android.os.Bundle"),
                    List.of("analyse_status", "analyse_progress", "download_paused")
            )
    );

    private GlobalProgressHookCatalog() {
    }

    public static List<Point> points() {
        return POINTS;
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

    public record Point(
            String id,
            String className,
            String methodName,
            String returnType,
            List<String> parameterTypes,
            List<String> requiredAnchors
    ) {
        public String packageName() {
            return className.substring(0, className.lastIndexOf('.'));
        }
    }
}

package com.wayne.hyperaicrbypass.hook;

import com.wayne.hyperaicrbypass.adapt.SemanticQuerySpec;
import com.wayne.hyperaicrbypass.config.Policy;

import java.util.List;
import java.util.Set;

public final class PreciseProgressHookCatalog {
    private static final List<Point> POINTS = List.of(
            point(
                    Kind.CAPTURE,
                    "com.xiaomi.aicr.searchpro.monitor.GalleryProgressMonitor",
                    "calculateProgress",
                    "int",
                    List.of("int", "int", "int", "int", "int", "int", "int", "int"),
                    Set.of("progress = ", " base：", "  numerator:")
            ),
            point(
                    Kind.TRANSPORT,
                    "com.xiaomi.aicr.searchpro.monitor.ProgressMonitor",
                    "getIndexProgress",
                    "android.os.Bundle",
                    List.of("int", "boolean", "kotlin.jvm.functions.Function3"),
                    Set.of("getIndexProgress scope:", "analyse_progress", "analyse_status")
            ),
            point(
                    Kind.NOTIFY,
                    "com.xiaomi.aicr.searchpro.monitor.RunningStatus",
                    "sendProgressToActivity",
                    "void",
                    List.of("int", "boolean"),
                    Set.of("enter sendProgressToActivity scopes：", "refresh_ui_progress",
                            "no ui scope Or no current scopes,no refresh")
            ),
            point(
                    Kind.DISPLAY,
                    "com.xiaomi.aicr.aisearch.progress.AISearchProgressActivity",
                    "refreshUI",
                    "void",
                    List.of("android.os.Bundle"),
                    Set.of("analyse_progress", "refreshUIStatus scope:")
            )
    );
    private static final List<Point> BRANCH_FALLBACKS = List.of(
            point(
                    Kind.CAPTURE,
                    "il2",
                    "a",
                    "int",
                    List.of("int", "int", "int", "int", "int", "int"),
                    true,
                    Set.of("GalleryProgressMonitor.calculateProgress", " base：",
                            "  numerator:")
            ),
            point(
                    Kind.TRANSPORT,
                    "ok5",
                    "g",
                    "android.os.Bundle",
                    List.of("int", "boolean", "oa6"),
                    false,
                    Set.of("ProgressMonitor.getMigratedProgress", "analyse_progress",
                            "analyse_status")
            ),
            point(
                    Kind.DISPLAY,
                    "com.xiaomi.aicr.aisearch.progress.AISearchProgressActivity",
                    "refreshUI$staticBridge",
                    "void",
                    List.of(
                            "com.xiaomi.aicr.aisearch.progress.AISearchProgressActivity",
                            "android.os.Bundle"
                    ),
                    true,
                    Set.of("analyse_progress", "analyse_status")
            )
    );
    private static final List<Point> V4_COMPACT_POINTS = List.of(
            point(
                    Kind.CAPTURE,
                    "ij3",
                    "a",
                    "int",
                    List.of("int", "int", "int", "int", "int", "int"),
                    true,
                    Set.of("GalleryProgressMonitor.calculateProgress", " base：",
                            "  numerator:")
            ),
            point(
                    Kind.TRANSPORT,
                    "ac7",
                    "g",
                    "android.os.Bundle",
                    List.of("int", "boolean", "rb8"),
                    false,
                    Set.of("ProgressMonitor.getMigratedProgress", "analyse_progress",
                            "analyse_status")
            ),
            point(
                    Kind.NOTIFY,
                    "qz7",
                    "J",
                    "void",
                    List.of("int", "boolean"),
                    false,
                    Set.of("RunningStatus.sendProgressToActivity",
                            "enter sendProgressToActivity scopes：")
            ),
            point(
                    Kind.DISPLAY,
                    "com.xiaomi.aicr.aisearch.progress.AISearchProgressActivity",
                    "l",
                    "void",
                    List.of(
                            "com.xiaomi.aicr.aisearch.progress.AISearchProgressActivity",
                            "android.os.Bundle"
                    ),
                    true,
                    Set.of("analyse_progress", "analyse_status")
            )
    );

    private PreciseProgressHookCatalog() {
    }

    public static List<Point> points() {
        return POINTS;
    }

    public static List<Point> branchFallbacks() {
        return BRANCH_FALLBACKS;
    }

    public static List<Point> points(AicrRuntimeLayout layout) {
        return layout == AicrRuntimeLayout.V4_COMPACT ? V4_COMPACT_POINTS : POINTS;
    }

    public static List<Point> branchFallbacks(AicrRuntimeLayout layout) {
        return layout == AicrRuntimeLayout.V4_COMPACT ? List.of() : BRANCH_FALLBACKS;
    }

    static boolean usesAssignableFunction3(AicrRuntimeLayout layout, Point point) {
        return layout == AicrRuntimeLayout.V4_COMPACT
                && point.kind() == Kind.TRANSPORT;
    }

    private static Point point(
            Kind kind,
            String className,
            String methodName,
            String returnType,
            List<String> parameterTypes,
            Set<String> anchors
    ) {
        return point(kind, className, methodName, returnType, parameterTypes, false, anchors);
    }

    private static Point point(
            Kind kind,
            String className,
            String methodName,
            String returnType,
            List<String> parameterTypes,
            boolean isStatic,
            Set<String> anchors
    ) {
        return new Point(
                kind,
                className,
                methodName,
                returnType,
                parameterTypes,
                new SemanticQuerySpec(
                        Policy.AI_UI_CAPABILITY,
                        "com.xiaomi.aicr",
                        returnType,
                        parameterTypes,
                        isStatic,
                        anchors
                )
        );
    }

    public enum Kind {
        CAPTURE,
        TRANSPORT,
        NOTIFY,
        DISPLAY
    }

    public record Point(
            Kind kind,
            String className,
            String methodName,
            String returnType,
            List<String> parameterTypes,
            SemanticQuerySpec semanticQuery
    ) {
        public Point {
            parameterTypes = List.copyOf(parameterTypes);
        }

        public String id() {
            return className + "#" + methodName + "(" + String.join(",", parameterTypes) + ")";
        }
    }
}

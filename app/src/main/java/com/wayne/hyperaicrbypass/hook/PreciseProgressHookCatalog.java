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

    private PreciseProgressHookCatalog() {
    }

    public static List<Point> points() {
        return POINTS;
    }

    private static Point point(
            Kind kind,
            String className,
            String methodName,
            String returnType,
            List<String> parameterTypes,
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
                        false,
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

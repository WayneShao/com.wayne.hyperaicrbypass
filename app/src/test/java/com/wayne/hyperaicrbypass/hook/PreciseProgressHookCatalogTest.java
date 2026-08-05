package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.Set;

public final class PreciseProgressHookCatalogTest {
    @Test
    public void definesExactCalculatorTransportNotificationAndDisplayShapes() {
        List<PreciseProgressHookCatalog.Point> points =
                PreciseProgressHookCatalog.points();

        assertEquals(4, points.size());
        assertPoint(points.get(0),
                PreciseProgressHookCatalog.Kind.CAPTURE,
                "com.xiaomi.aicr.searchpro.monitor.GalleryProgressMonitor",
                "calculateProgress",
                "int",
                List.of("int", "int", "int", "int", "int", "int", "int", "int"),
                Set.of("progress = ", " base：", "  numerator:"));
        assertPoint(points.get(1),
                PreciseProgressHookCatalog.Kind.TRANSPORT,
                "com.xiaomi.aicr.searchpro.monitor.ProgressMonitor",
                "getIndexProgress",
                "android.os.Bundle",
                List.of("int", "boolean", "kotlin.jvm.functions.Function3"),
                Set.of("getIndexProgress scope:", "analyse_progress", "analyse_status"));
        assertPoint(points.get(2),
                PreciseProgressHookCatalog.Kind.NOTIFY,
                "com.xiaomi.aicr.searchpro.monitor.RunningStatus",
                "sendProgressToActivity",
                "void",
                List.of("int", "boolean"),
                Set.of("enter sendProgressToActivity scopes：", "refresh_ui_progress",
                        "no ui scope Or no current scopes,no refresh"));
        assertPoint(points.get(3),
                PreciseProgressHookCatalog.Kind.DISPLAY,
                "com.xiaomi.aicr.aisearch.progress.AISearchProgressActivity",
                "refreshUI",
                "void",
                List.of("android.os.Bundle"),
                Set.of("analyse_progress", "refreshUIStatus scope:"));
    }

    @Test
    public void everyFallbackIsAnInstanceMethodWithStrictAnchors() {
        for (PreciseProgressHookCatalog.Point point : PreciseProgressHookCatalog.points()) {
            assertFalse(point.semanticQuery().isStatic());
            assertEquals("com.xiaomi.aicr", point.semanticQuery().packagePrefix());
            assertFalse(point.semanticQuery().requiredAnchors().isEmpty());
            assertEquals(point.returnType(), point.semanticQuery().returnType());
            assertEquals(point.parameterTypes(), point.semanticQuery().parameterTypes());
        }
    }

    private static void assertPoint(
            PreciseProgressHookCatalog.Point point,
            PreciseProgressHookCatalog.Kind kind,
            String className,
            String methodName,
            String returnType,
            List<String> parameterTypes,
            Set<String> anchors
    ) {
        assertEquals(kind, point.kind());
        assertEquals(className, point.className());
        assertEquals(methodName, point.methodName());
        assertEquals(returnType, point.returnType());
        assertEquals(parameterTypes, point.parameterTypes());
        assertEquals(anchors, point.semanticQuery().requiredAnchors());
        assertTrue(point.id().contains(methodName));
    }
}

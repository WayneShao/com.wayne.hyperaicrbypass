package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.Set;

public final class PreciseProgressHookCatalogTest {
    @Test
    public void globalCatalogUsesVersionSpecificShapes() {
        assertEquals(8, GlobalProgressHookCatalog.points(AicrVersionBranch.V3).size());
        assertEquals(11, GlobalProgressHookCatalog.points(AicrVersionBranch.V4).size());
        assertTrue(GlobalProgressHookCatalog.points(AicrVersionBranch.V3).stream()
                .filter(point -> point.id().equals("local-calculator"))
                .findFirst().orElseThrow().isStatic());
        assertFalse(GlobalProgressHookCatalog.points(AicrVersionBranch.V4).stream()
                .filter(point -> point.id().equals("local-calculator"))
                .findFirst().orElseThrow().isStatic());
    }

    @Test
    public void compactV4CatalogTracksR8ProgressOwnersAndShapes() {
        List<PreciseProgressHookCatalog.Point> precise =
                PreciseProgressHookCatalog.points(AicrRuntimeLayout.V4_COMPACT);
        List<GlobalProgressHookCatalog.Point> global =
                GlobalProgressHookCatalog.points(AicrRuntimeLayout.V4_COMPACT);

        assertEquals(List.of("ij3", "ac7", "qz7",
                        "com.xiaomi.aicr.aisearch.progress.AISearchProgressActivity"),
                precise.stream().map(PreciseProgressHookCatalog.Point::className).toList());
        assertEquals(8, global.size());
        assertTrue(global.stream().anyMatch(point ->
                point.id().equals("local-calculator")
                        && point.className().equals("ac7")
                        && point.isStatic()));
        assertTrue(global.stream().anyMatch(point ->
                point.id().equals("gallery-calculator")
                        && point.className().equals("ij3")
                        && point.parameterTypes().size() == 6));
        assertTrue(global.stream().anyMatch(point ->
                point.id().equals("setting-display")
                        && point.parameterTypes().size() == 2
                        && point.isStatic()));
        assertTrue(PreciseProgressHookCatalog.usesAssignableFunction3(
                AicrRuntimeLayout.V4_COMPACT,
                precise.stream()
                        .filter(point -> point.kind()
                                == PreciseProgressHookCatalog.Kind.TRANSPORT)
                        .findFirst().orElseThrow()
        ));
        assertTrue(GlobalProgressHookCatalog.usesAssignableFunction3(
                AicrRuntimeLayout.V4_COMPACT,
                global.stream().filter(point -> point.id().equals("index"))
                        .findFirst().orElseThrow()
        ));
    }

    @Test
    public void discoverySchemaChangesForTheCompactRuntimeLayout() {
        assertEquals(4, ExecutionCoverageReporter.SCHEMA_REVISION);
    }

    @Test
    public void unanchoredOutgoingBridgeStaysOnDiscoveredProgressOwner() {
        GlobalProgressHookCatalog.Point outgoing =
                GlobalProgressHookCatalog.points(AicrRuntimeLayout.V4_COMPACT).stream()
                        .filter(point -> point.id().equals("outgoing-bridge"))
                        .findFirst().orElseThrow();

        assertTrue(GlobalPreciseProgressHooks.matchesExpectedOwner(
                outgoing, "zz1", "zz1", AicrRuntimeLayout.V4_COMPACT));
        assertFalse(GlobalPreciseProgressHooks.matchesExpectedOwner(
                outgoing, "third.party.Callbacks", "zz1",
                AicrRuntimeLayout.V4_COMPACT));
    }

    @Test
    public void globalProgressRescanDoesNotRegisterAnInstalledPointAgain() {
        assertTrue(GlobalPreciseProgressHooks.requiresRegistration(
                Set.of(), "index"));
        assertFalse(GlobalPreciseProgressHooks.requiresRegistration(
                Set.of("index"), "index"));
    }

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
    public void everyFallbackHasStrictAnchorsAndBranchSpecificShapes() {
        for (PreciseProgressHookCatalog.Point point : PreciseProgressHookCatalog.points()) {
            assertFalse(point.semanticQuery().isStatic());
            assertEquals("com.xiaomi.aicr", point.semanticQuery().packagePrefix());
            assertFalse(point.semanticQuery().requiredAnchors().isEmpty());
            assertEquals(point.returnType(), point.semanticQuery().returnType());
            assertEquals(point.parameterTypes(), point.semanticQuery().parameterTypes());
        }

        PreciseProgressHookCatalog.Point displayBridge =
                PreciseProgressHookCatalog.branchFallbacks().stream()
                        .filter(point -> point.kind() == PreciseProgressHookCatalog.Kind.DISPLAY)
                        .findFirst().orElseThrow();
        assertTrue(displayBridge.semanticQuery().isStatic());
        assertEquals(List.of(
                "com.xiaomi.aicr.aisearch.progress.AISearchProgressActivity",
                "android.os.Bundle"
        ), displayBridge.parameterTypes());
        assertTrue(displayBridge.semanticQuery().requiredAnchors().contains("analyse_progress"));

        PreciseProgressHookCatalog.Point v3Capture =
                PreciseProgressHookCatalog.branchFallbacks().stream()
                        .filter(point -> point.kind() == PreciseProgressHookCatalog.Kind.CAPTURE)
                        .findFirst().orElseThrow();
        assertEquals("il2", v3Capture.className());
        assertEquals(List.of("int", "int", "int", "int", "int", "int"),
                v3Capture.parameterTypes());
        assertTrue(v3Capture.semanticQuery().isStatic());

        PreciseProgressHookCatalog.Point v3Transport =
                PreciseProgressHookCatalog.branchFallbacks().stream()
                        .filter(point -> point.kind() == PreciseProgressHookCatalog.Kind.TRANSPORT)
                        .findFirst().orElseThrow();
        assertEquals("ok5", v3Transport.className());
        assertEquals(List.of("int", "boolean", "oa6"), v3Transport.parameterTypes());
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

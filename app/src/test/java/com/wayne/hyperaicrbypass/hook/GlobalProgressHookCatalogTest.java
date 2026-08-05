package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

public final class GlobalProgressHookCatalogTest {
    @Test
    public void coversEveryCurrentBranchCaptureTransportAndDisplayPoint() {
        Set<String> ids = GlobalProgressHookCatalog.points().stream()
                .map(GlobalProgressHookCatalog.Point::id)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "index",
                "migrated",
                "unmigrated",
                "local-scope",
                "local-calculator",
                "gallery-boundary",
                "gallery-calculator",
                "gallery-postprocess",
                "notification",
                "outgoing-bridge",
                "setting-display"
        ), ids);
        assertEquals(ids.size(), GlobalProgressHookCatalog.points().size());
    }

    @Test
    public void ambiguousBranchShapesHaveDistinctSemanticAnchors() {
        GlobalProgressHookCatalog.Point migrated = point("migrated");
        GlobalProgressHookCatalog.Point unmigrated = point("unmigrated");

        assertEquals(migrated.parameterTypes(), unmigrated.parameterTypes());
        assertFalse(migrated.requiredAnchors().isEmpty());
        assertFalse(unmigrated.requiredAnchors().isEmpty());
        assertFalse(migrated.requiredAnchors().equals(unmigrated.requiredAnchors()));
    }

    @Test
    public void readinessIsIndependentForEachRealCalculationBranch() {
        Set<String> direct = GlobalProgressHookCatalog.requiredPointIds(
                GlobalProgressBranch.MIGRATED_DIRECT_AI);
        Set<String> postprocessed = GlobalProgressHookCatalog.requiredPointIds(
                GlobalProgressBranch.MIGRATED_POSTPROCESSED);
        Set<String> unmigrated = GlobalProgressHookCatalog.requiredPointIds(
                GlobalProgressBranch.UNMIGRATED_LOCAL);

        assertFalse(direct.contains("gallery-postprocess"));
        assertFalse(direct.contains("unmigrated"));
        assertFalse(unmigrated.contains("migrated"));
        assertFalse(unmigrated.contains("gallery-calculator"));
        assertFalse(unmigrated.contains("gallery-boundary"));
        assertEquals(true, direct.contains("notification"));
        assertEquals(true, unmigrated.contains("notification"));
        assertEquals(Set.of("gallery-postprocess"), postprocessed.stream()
                .filter(id -> !direct.contains(id))
                .collect(Collectors.toSet()));
    }

    private static GlobalProgressHookCatalog.Point point(String id) {
        return GlobalProgressHookCatalog.points().stream()
                .filter(point -> point.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}

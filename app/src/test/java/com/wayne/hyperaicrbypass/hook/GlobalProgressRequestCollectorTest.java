package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public final class GlobalProgressRequestCollectorTest {
    @Test
    public void buildsOnlyFromTheCurrentCompleteDirectAiRequest() {
        GlobalProgressRequestCollector collector = new GlobalProgressRequestCollector();
        GlobalProgressRequestCollector.IndexToken request = collector.beginIndex(31, false);

        captureLocal(collector, 2, new Object[]{1, 1, 0}, 50);
        captureLocal(collector, 4, new Object[]{2, 2, 0}, 50);
        captureLocal(collector, 8, new Object[]{2, 1, 1}, 75);
        captureLocal(collector, 16, new Object[]{3, 2, 1}, 66);

        GlobalProgressRequestCollector.GalleryToken gallery = collector.beginGallery();
        collector.captureGallery(
                new Object[]{0, 3, 0, 0, 0, 0, 1, 0}, 33
        );
        collector.finishGallery(gallery, 33);
        collector.markMigratedDirect();

        GlobalProgressSnapshot snapshot = collector.finishIndex(
                request, 54, 12_345L, 9_000L
        ).orElseThrow();

        assertEquals(55_000, snapshot.thousandthsPercent());
        assertEquals(54, snapshot.fixedProgress());
        assertFalse(collector.hasActiveRequest());
    }

    @Test
    public void rejectsIncompleteAndAlwaysClearsTheRequest() {
        GlobalProgressRequestCollector collector = new GlobalProgressRequestCollector();
        GlobalProgressRequestCollector.IndexToken request = collector.beginIndex(31, false);
        captureLocal(collector, 2, new Object[]{1, 1, 0}, 50);
        collector.markMigratedDirect();

        assertFalse(collector.finishIndex(
                request, 10, 12_345L, 9_000L
        ).isPresent());
        assertFalse(collector.hasActiveRequest());
    }

    @Test
    public void buildsTheUnmigratedLocalBranchWithoutGalleryData() {
        GlobalProgressRequestCollector collector = new GlobalProgressRequestCollector();
        GlobalProgressRequestCollector.IndexToken request = collector.beginIndex(31, false);
        captureLocals(collector);
        collector.markUnmigratedLocal();

        GlobalProgressSnapshot snapshot = collector.finishIndex(
                request, 60, 12_345L, 9_000L
        ).orElseThrow();

        assertEquals(GlobalProgressBranch.UNMIGRATED_LOCAL, snapshot.branch());
        assertEquals(60_417, snapshot.thousandthsPercent());
    }

    @Test
    public void acceptsPostprocessingOnlyInsideTheSameGalleryBoundary() {
        GlobalProgressRequestCollector collector = new GlobalProgressRequestCollector();
        GlobalProgressRequestCollector.IndexToken request = collector.beginIndex(31, false);
        captureLocals(collector);
        GlobalProgressRequestCollector.GalleryToken gallery = collector.beginGallery();
        collector.captureGallery(
                new Object[]{0, 3, 0, 0, 0, 0, 1, 0}, 33
        );
        collector.captureMigrationPostprocess(
                new Object[]{50.0f, 33, 50, 100, 100}, 83
        );
        collector.finishGallery(gallery, 83);
        collector.markMigratedDirect();

        GlobalProgressSnapshot snapshot = collector.finishIndex(
                request, 64, 12_345L, 9_000L
        ).orElseThrow();

        assertEquals(GlobalProgressBranch.MIGRATED_POSTPROCESSED, snapshot.branch());
        assertEquals(65_000, snapshot.thousandthsPercent());
    }

    @Test
    public void representsProvedGalleryEarlyCompletionWithoutFakeIntegerPrecision() {
        GlobalProgressRequestCollector collector = new GlobalProgressRequestCollector();
        GlobalProgressRequestCollector.IndexToken request = collector.beginIndex(31, false);
        captureLocals(collector);
        GlobalProgressRequestCollector.GalleryToken gallery = collector.beginGallery();
        collector.finishGallery(gallery, 100);
        collector.markMigratedDirect();

        GlobalProgressSnapshot snapshot = collector.finishIndex(
                request, 68, 12_345L, 9_000L
        ).orElseThrow();

        assertEquals(68_333, snapshot.thousandthsPercent());
        assertEquals(68, snapshot.fixedProgress());
    }

    private static void captureLocal(
            GlobalProgressRequestCollector collector,
            int scope,
            Object[] args,
            int fixed
    ) {
        GlobalProgressRequestCollector.ScopeToken token = collector.beginScope(scope);
        collector.captureLocal(args);
        collector.finishScope(token, fixed);
    }

    private static void captureLocals(GlobalProgressRequestCollector collector) {
        captureLocal(collector, 2, new Object[]{1, 1, 0}, 50);
        captureLocal(collector, 4, new Object[]{2, 2, 0}, 50);
        captureLocal(collector, 8, new Object[]{2, 1, 1}, 75);
        captureLocal(collector, 16, new Object[]{3, 2, 1}, 66);
    }
}

package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

import com.wayne.hyperaicrbypass.config.ProgressPrecision;

public final class GlobalProgressDisplayTest {
    @Test
    public void missingRunningProgressRendersARealLoadingState() {
        Optional<GlobalProgressDisplay.LoadingPlan> plan =
                GlobalProgressDisplay.loadingPlan(31, -1, true, false, false);

        assertTrue(plan.isPresent());
        assertEquals("...", plan.get().buttonText());
        assertFalse(plan.get().enabled());
    }

    @Test
    public void plansTheSamePreciseValueForDescriptionButtonAndAccessibility() {
        GlobalProgressSnapshot snapshot = snapshot();

        GlobalProgressDisplay.RenderPlan plan = GlobalProgressDisplay.plan(
                "已完成85%，分析过程中可继续使用相册",
                "85%",
                "85%",
                31,
                1,
                85,
                snapshot,
                12_345L,
                9_000L
        ).orElseThrow();

        assertEquals("已完成85.317%，分析过程中可继续使用相册", plan.description());
        assertEquals("85.317%", plan.buttonText());
        assertEquals("85.317%", plan.contentDescription());
    }

    @Test
    public void pausedStateReplacesOnlyFieldsThatContainNativePercentage() {
        GlobalProgressSnapshot snapshot = snapshot();

        GlobalProgressDisplay.RenderPlan plan = GlobalProgressDisplay.plan(
                "已完成85%", "已暂停", "已暂停",
                31, 0, 85, snapshot, 12_345L, 9_000L
        ).orElseThrow();

        assertEquals("已完成85.317%", plan.description());
        assertEquals("已暂停", plan.buttonText());
        assertEquals("已暂停", plan.contentDescription());
    }

    @Test
    public void globalSurfaceUsesTheSameConfiguredPrecision() {
        GlobalProgressSnapshot snapshot = snapshot();

        assertEquals("85.3%", GlobalProgressDisplay.format(
                snapshot, ProgressPrecision.TENTHS));
        assertEquals("85.32%", GlobalProgressDisplay.format(
                snapshot, ProgressPrecision.HUNDREDTHS));
        assertEquals("85.317%", GlobalProgressDisplay.format(
                snapshot, ProgressPrecision.THOUSANDTHS));
        assertFalse(GlobalProgressDisplay.plan(
                "已完成85%", "85%", "85%",
                31, 1, 85, snapshot, 12_345L, 9_000L,
                ProgressPrecision.ORIGINAL
        ).isPresent());
    }

    @Test
    public void rejectsWrongRunNativeMismatchCompletionAndMissingExactData() {
        GlobalProgressSnapshot snapshot = snapshot();

        assertFalse(GlobalProgressDisplay.plan(
                "已完成85%", "85%", "85%",
                31, 1, 85, snapshot, 12_346L, 9_000L
        ).isPresent());
        assertFalse(GlobalProgressDisplay.plan(
                "已完成84%", "84%", "84%",
                31, 1, 84, snapshot, 12_345L, 9_000L
        ).isPresent());
        assertFalse(GlobalProgressDisplay.plan(
                "已完成100%", "100%", "100%",
                31, 1, 100, snapshot, 12_345L, 9_000L
        ).isPresent());
        assertFalse(GlobalProgressDisplay.plan(
                "已完成85%", "85%", "85%",
                31, 1, 85, null, 12_345L, 9_000L
        ).isPresent());
        assertFalse(GlobalProgressDisplay.plan(
                "等待开始", "开始", "开始",
                31, 0, 85, snapshot, 12_345L, 9_000L
        ).isPresent());
    }

    private static GlobalProgressSnapshot snapshot() {
        return new GlobalProgressSnapshot(
                85_317, 85, GlobalProgressBranch.MIGRATED_DIRECT_AI,
                12_345L, 7L, 8_000L
        );
    }
}

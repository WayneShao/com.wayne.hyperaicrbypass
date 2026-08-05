package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public final class GlobalProgressDisplayTest {
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
    public void rejectsPausedWrongRunAndNativeMismatchStates() {
        GlobalProgressSnapshot snapshot = snapshot();

        assertFalse(GlobalProgressDisplay.plan(
                "已完成85%", "已暂停", "已暂停",
                31, 1, 85, snapshot, 12_345L, 9_000L
        ).isPresent());
        assertFalse(GlobalProgressDisplay.plan(
                "已完成85%", "85%", "85%",
                31, 1, 85, snapshot, 12_346L, 9_000L
        ).isPresent());
        assertFalse(GlobalProgressDisplay.plan(
                "已完成84%", "84%", "84%",
                31, 1, 84, snapshot, 12_345L, 9_000L
        ).isPresent());
    }

    private static GlobalProgressSnapshot snapshot() {
        return new GlobalProgressSnapshot(
                85_317, 85, GlobalProgressBranch.MIGRATED_DIRECT_AI,
                12_345L, 7L, 8_000L
        );
    }
}

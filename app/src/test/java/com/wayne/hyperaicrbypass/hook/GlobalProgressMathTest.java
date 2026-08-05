package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public final class GlobalProgressMathTest {
    @Test
    public void preservesFractionsAcrossAicrsTwoNativeTruncations() {
        GlobalProgressComponent gallery = component(1, 3, 33);
        GlobalProgressComponent note = component(1, 2, 50);
        GlobalProgressComponent message = component(2, 3, 66);
        GlobalProgressComponent file = component(3, 4, 75);
        GlobalProgressComponent record = component(4, 5, 80);

        GlobalProgressSnapshot snapshot = GlobalProgressMath.migratedDirect(
                gallery, note, message, file, record,
                60, 12_345L, 7L, 8_000L
        ).orElseThrow();

        assertEquals(61_000, snapshot.thousandthsPercent());
        assertEquals(60, snapshot.fixedProgress());
        assertEquals(GlobalProgressBranch.MIGRATED_DIRECT_AI, snapshot.branch());
        assertEquals("61.000%", GlobalProgressDisplay.format(snapshot));
    }

    @Test
    public void rejectsAResultThatDoesNotReplayTheNativeInteger() {
        GlobalProgressComponent value = component(1, 2, 50);

        assertFalse(GlobalProgressMath.migratedDirect(
                value, value, value, value, value,
                49, 1L, 1L, 1L
        ).isPresent());
    }

    @Test
    public void preservesLocalFractionsOnTheUnmigratedBranch() {
        GlobalProgressSnapshot snapshot = GlobalProgressMath.unmigratedLocal(
                component(1, 2, 50),
                component(2, 4, 50),
                component(3, 4, 75),
                component(4, 6, 66),
                60, 12_345L, 8L, 9_000L
        ).orElseThrow();

        assertEquals(60_417, snapshot.thousandthsPercent());
        assertEquals(60, snapshot.fixedProgress());
        assertEquals(GlobalProgressBranch.UNMIGRATED_LOCAL, snapshot.branch());
    }

    @Test
    public void preservesTheAiFractionThroughGalleryMigrationPostprocessing() {
        GlobalProgressSnapshot snapshot = GlobalProgressMath.migratedPostprocessed(
                component(1, 3, 33),
                50.0f,
                50,
                100,
                100,
                83,
                component(1, 2, 50),
                component(2, 4, 50),
                component(3, 4, 75),
                component(4, 6, 66),
                64,
                12_345L,
                9L,
                9_000L
        ).orElseThrow();

        assertEquals(65_000, snapshot.thousandthsPercent());
        assertEquals(64, snapshot.fixedProgress());
        assertEquals(GlobalProgressBranch.MIGRATED_POSTPROCESSED, snapshot.branch());
    }

    private static GlobalProgressComponent component(
            long numerator,
            long denominator,
            int fixed
    ) {
        return GlobalProgressComponent.restore(numerator, denominator, fixed).orElseThrow();
    }
}

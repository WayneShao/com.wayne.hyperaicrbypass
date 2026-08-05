package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GlobalProgressHookLogicTest {
    @Test
    public void migratedBranchUsesGalleryAndFourLocalContributorScopes() {
        GlobalProgressSnapshot snapshot = snapshot(
                GlobalProgressBranch.MIGRATED_DIRECT_AI);

        for (int scope : new int[]{1, 2, 4, 8, 16}) {
            assertTrue(GlobalProgressHookLogic.shouldForceNotification(
                    true, scope, snapshot, 12_345L, 9_000L));
        }
        assertFalse(GlobalProgressHookLogic.shouldForceNotification(
                true, 32, snapshot, 12_345L, 9_000L));
    }

    @Test
    public void unmigratedBranchExcludesGalleryAndRejectsStaleOrWrongRunData() {
        GlobalProgressSnapshot snapshot = snapshot(
                GlobalProgressBranch.UNMIGRATED_LOCAL);

        assertFalse(GlobalProgressHookLogic.shouldForceNotification(
                true, 1, snapshot, 12_345L, 9_000L));
        assertTrue(GlobalProgressHookLogic.shouldForceNotification(
                true, 2, snapshot, 12_345L, 9_000L));
        assertFalse(GlobalProgressHookLogic.shouldForceNotification(
                false, 2, snapshot, 12_345L, 9_000L));
        assertFalse(GlobalProgressHookLogic.shouldForceNotification(
                true, 2, snapshot, 12_346L, 9_000L));
        assertFalse(GlobalProgressHookLogic.shouldForceNotification(
                true, 2, snapshot, 12_345L, 369_001L));
    }

    private static GlobalProgressSnapshot snapshot(GlobalProgressBranch branch) {
        return new GlobalProgressSnapshot(
                85_392, 85, branch, 12_345L, 1L, 9_000L
        );
    }
}

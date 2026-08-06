package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GlobalProgressHookLogicTest {
    @Test
    public void runningInitiativeWinsOverStaleStoppedStatusBeforeRendering() {
        assertEquals(4, GlobalProgressHookLogic.normalizeUiStatus(
                31, 0, 98, true, false, false));
    }

    @Test
    public void explicitPauseIsNeverNormalizedToRunning() {
        assertEquals(0, GlobalProgressHookLogic.normalizeUiStatus(
                31, 0, 98, true, true, false));
        assertEquals(0, GlobalProgressHookLogic.normalizeUiStatus(
                31, 0, 98, true, false, true));
    }

    @Test
    public void unrelatedScopesAndSettledStatusesRemainUnchanged() {
        assertEquals(0, GlobalProgressHookLogic.normalizeUiStatus(
                1, 0, 98, true, false, false));
        assertEquals(4, GlobalProgressHookLogic.normalizeUiStatus(
                31, 4, 98, true, false, false));
    }

    @Test
    public void missingProgressUsesLoadingStateInsteadOfRenderingNegativePercent() {
        assertEquals(-1, GlobalProgressHookLogic.normalizeUiStatus(
                31, 4, -1, true, false, false));
    }

    @Test
    public void migratedChainUsesGalleryLocalAndDirectGlobalContributorScopes() {
        for (int scope : new int[]{1, 2, 4, 8, 16, 31}) {
            assertTrue(GlobalProgressHookLogic.shouldForceNotification(
                    true, false, scope));
        }
        assertFalse(GlobalProgressHookLogic.shouldForceNotification(
                true, false, 32));
    }

    @Test
    public void unmigratedChainExcludesGalleryAndDoesNotNeedPriorSnapshot() {
        assertFalse(GlobalProgressHookLogic.shouldForceNotification(
                false, true, 1));
        for (int scope : new int[]{2, 4, 8, 16, 31}) {
            assertTrue(GlobalProgressHookLogic.shouldForceNotification(
                    false, true, scope));
        }
        assertFalse(GlobalProgressHookLogic.shouldForceNotification(
                false, false, 2));
    }
}

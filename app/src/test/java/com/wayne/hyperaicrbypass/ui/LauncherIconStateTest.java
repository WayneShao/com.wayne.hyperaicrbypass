package com.wayne.hyperaicrbypass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.pm.PackageManager;

import org.junit.Test;

public final class LauncherIconStateTest {
    @Test
    public void manifestDefaultAndExplicitEnabledAreVisible() {
        assertTrue(LauncherIconState.isVisible(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT));
        assertTrue(LauncherIconState.isVisible(PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
    }

    @Test
    public void disabledStatesAreHidden() {
        assertFalse(LauncherIconState.isVisible(PackageManager.COMPONENT_ENABLED_STATE_DISABLED));
        assertFalse(LauncherIconState.isVisible(
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
        ));
        assertFalse(LauncherIconState.isVisible(
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
        ));
    }

    @Test
    public void visibilityMapsToExplicitComponentState() {
        assertEquals(
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                LauncherIconState.componentState(true)
        );
        assertEquals(
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                LauncherIconState.componentState(false)
        );
    }
}

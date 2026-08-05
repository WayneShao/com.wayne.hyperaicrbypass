package com.wayne.hyperaicrbypass.ui;

import android.content.pm.PackageManager;

public final class LauncherIconState {
    private LauncherIconState() {
    }

    public static boolean isVisible(int componentState) {
        return componentState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                || componentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    public static int componentState(boolean visible) {
        return visible
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }
}

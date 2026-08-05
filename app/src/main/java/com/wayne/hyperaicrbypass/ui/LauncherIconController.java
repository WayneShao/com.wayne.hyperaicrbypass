package com.wayne.hyperaicrbypass.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

public final class LauncherIconController {
    private final PackageManager packageManager;
    private final ComponentName launcherAlias;

    public LauncherIconController(Context context) {
        packageManager = context.getPackageManager();
        launcherAlias = new ComponentName(
                context.getPackageName(), context.getPackageName() + ".LauncherAlias"
        );
    }

    public boolean isVisible() {
        return LauncherIconState.isVisible(
                packageManager.getComponentEnabledSetting(launcherAlias)
        );
    }

    public void setVisible(boolean visible) {
        packageManager.setComponentEnabledSetting(
                launcherAlias,
                LauncherIconState.componentState(visible),
                PackageManager.DONT_KILL_APP
        );
    }
}

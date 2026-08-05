package com.wayne.hyperaicrbypass;

import com.wayne.hyperaicrbypass.hook.HookBootstrap;

import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "HyperAICRBypass";
    private static final Set<String> TARGET_PACKAGES = Set.of(
            "com.miui.gallery",
            "com.xiaomi.aicr",
            "com.xiaomi.aiservice"
    );

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGES.contains(lpparam.packageName)) {
            return;
        }
        XposedBridge.log(TAG + ": load " + lpparam.packageName + " process=" + lpparam.processName);
        HookBootstrap.installAfterAttach(lpparam.packageName, lpparam.processName);
    }
}

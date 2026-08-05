package com.wayne.hyperaicrbypass;

import android.util.Log;

import com.wayne.hyperaicrbypass.hook.HookBootstrap;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;

import java.util.Set;

import io.github.libxposed.api.XposedModule;

public final class MainHook extends XposedModule {
    private static final String TAG = "HyperAICRBypass";
    private static final Set<String> TARGET_PACKAGES = Set.of(
            "com.miui.gallery",
            "com.xiaomi.aicr",
            "com.xiaomi.aiservice"
    );

    private String processName;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        ModernXposed.initialize(this);
        processName = param.getProcessName();
        log(Log.INFO, TAG, "module loaded in " + processName);
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        String packageName = param.getPackageName();
        if (!TARGET_PACKAGES.contains(packageName)) {
            return;
        }
        log(Log.INFO, TAG, "load " + packageName + " process=" + processName);
        HookBootstrap.installAfterAttach(packageName, processName);
    }
}

package com.wayne.hyperaicrbypass.hook;

import com.wayne.hyperaicrbypass.config.ConfigClient;

import com.wayne.hyperaicrbypass.xposed.ModernHook;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;
import com.wayne.hyperaicrbypass.xposed.ReflectionHelpers;

public final class RunningStatusCompatibilityHooks {
    private static final String TAG = "HyperAICRBypass";

    private final ClassLoader classLoader;
    private final ConfigClient configClient;

    public RunningStatusCompatibilityHooks(ClassLoader classLoader, ConfigClient configClient) {
        this.classLoader = classLoader;
        this.configClient = configClient;
    }

    public int install() {
        int installed = 0;
        installed += installPutInt() ? 1 : 0;
        installed += installGetInt() ? 1 : 0;
        ModernXposed.log(TAG + ": running-status compatibility hooks=" + installed + "/2");
        return installed;
    }

    private boolean installPutInt() {
        try {
            ReflectionHelpers.findAndHookMethod(
                    "android.app.SharedPreferencesImpl$EditorImpl",
                    classLoader,
                    "putInt",
                    String.class,
                    int.class,
                    new ModernHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String key = param.args[0] instanceof String value ? value : null;
                            Integer original = param.args[1] instanceof Integer value ? value : null;
                            if (key == null || original == null) {
                                return;
                            }
                            int normalized = RunningStatusCompatibility.normalize(
                                    configClient.snapshot(), key, original
                            );
                            if (normalized != original) {
                                param.args[1] = normalized;
                                ModernXposed.log(TAG + ": putInt(runningStatus,-2) -> 0");
                            }
                        }
                    }
            );
            return true;
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": running-status putInt hook unavailable -> " + error);
            return false;
        }
    }

    private boolean installGetInt() {
        try {
            ReflectionHelpers.findAndHookMethod(
                    "android.app.SharedPreferencesImpl",
                    classLoader,
                    "getInt",
                    String.class,
                    int.class,
                    new ModernHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String key = param.args[0] instanceof String value ? value : null;
                            Integer original = param.getResult() instanceof Integer value
                                    ? value : null;
                            if (key == null || original == null) {
                                return;
                            }
                            int normalized = RunningStatusCompatibility.normalize(
                                    configClient.snapshot(), key, original
                            );
                            if (normalized != original) {
                                param.setResult(normalized);
                                ModernXposed.log(TAG + ": getInt(runningStatus) -2 -> 0");
                            }
                        }
                    }
            );
            return true;
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": running-status getInt hook unavailable -> " + error);
            return false;
        }
    }
}

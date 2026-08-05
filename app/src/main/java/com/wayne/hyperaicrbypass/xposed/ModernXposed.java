package com.wayne.hyperaicrbypass.xposed;

import android.util.Log;

import java.lang.reflect.Executable;

import io.github.libxposed.api.XposedInterface;

public final class ModernXposed {
    private static final String TAG = "HyperAICRBypass";
    private static volatile XposedInterface api;

    private ModernXposed() {
    }

    public static void initialize(XposedInterface xposed) {
        api = xposed;
    }

    public static XposedInterface.HookHandle hookMethod(
            Executable executable,
            ModernHook callback
    ) {
        XposedInterface xposed = requireApi();
        return xposed.hook(executable).intercept(chain -> {
            ModernHook.MethodHookParam param = new ModernHook.MethodHookParam(
                    chain.getThisObject(),
                    chain.getArgs().toArray()
            );
            try {
                callback.beforeHookedMethod(param);
            } catch (Throwable error) {
                logCallbackFailure("before", executable, error);
            }

            if (!param.returnEarly()) {
                try {
                    param.result(chain.proceed(param.args));
                } catch (Throwable error) {
                    param.throwable(error);
                }
            }

            try {
                callback.afterHookedMethod(param);
            } catch (Throwable error) {
                logCallbackFailure("after", executable, error);
            }

            if (param.throwable() != null) {
                throw param.throwable();
            }
            return param.result();
        });
    }

    public static void log(String message) {
        XposedInterface xposed = api;
        if (xposed == null) {
            Log.i(TAG, message);
            return;
        }
        xposed.log(Log.INFO, TAG, message);
    }

    public static void log(String message, Throwable error) {
        XposedInterface xposed = api;
        if (xposed == null) {
            Log.e(TAG, message, error);
            return;
        }
        xposed.log(Log.ERROR, TAG, message, error);
    }

    private static XposedInterface requireApi() {
        XposedInterface xposed = api;
        if (xposed == null) {
            throw new IllegalStateException("Modern Xposed API is not initialized");
        }
        return xposed;
    }

    private static void logCallbackFailure(
            String phase,
            Executable executable,
            Throwable error
    ) {
        log("Hook " + phase + " callback failed for " + executable, error);
    }
}

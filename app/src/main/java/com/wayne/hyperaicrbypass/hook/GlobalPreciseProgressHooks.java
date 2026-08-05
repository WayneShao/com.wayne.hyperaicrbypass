package com.wayne.hyperaicrbypass.hook;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import com.wayne.hyperaicrbypass.config.ConfigClient;
import com.wayne.hyperaicrbypass.config.Policy;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class GlobalPreciseProgressHooks {
    private static final String TAG = "HyperAICRBypass";
    private static final String PROGRESS = "analyse_progress";
    private static final String GLOBAL_PROGRESS = "global_analyse_progress";
    private static final String STATUS = "analyse_status";
    private static final String SCOPE = "scope";
    private static final int REQUIRED_HOOKS = 8;

    private final ClassLoader classLoader;
    private final ConfigClient configClient;
    private final GlobalProgressRequestCollector collector =
            new GlobalProgressRequestCollector();
    private final AtomicReference<GlobalProgressSnapshot> latest =
            new AtomicReference<>();
    private volatile boolean chainReady;
    private Object runningStatus;

    public GlobalPreciseProgressHooks(Context context, ConfigClient configClient) {
        this.classLoader = context.getClassLoader();
        this.configClient = configClient;
    }

    public int install() {
        chainReady = false;
        int installed = 0;
        try {
            Class<?> progress = find("com.xiaomi.aicr.searchpro.monitor.ProgressMonitor");
            Class<?> gallery = find(
                    "com.xiaomi.aicr.searchpro.monitor.GalleryProgressMonitor");
            Class<?> activity = find("com.xiaomi.aicr.aisearch.AiSearchSettingActivity");
            Class<?> function3 = find("kotlin.jvm.functions.Function3");
            runningStatus = XposedHelpers.getStaticObjectField(
                    find("com.xiaomi.aicr.searchpro.monitor.RunningStatus"), "INSTANCE");

            installed += hook(progress, "getIndexProgress", Bundle.class,
                    new Class<?>[]{int.class, boolean.class, function3},
                    indexCallback(), "index");
            installed += hook(progress, "getMigratedProgress", int.class,
                    new Class<?>[]{int.class, boolean.class, function3},
                    migratedCallback(), "migrated");
            installed += hook(progress, "calculateScopeProgress", int.class,
                    new Class<?>[]{int.class, boolean.class, boolean.class, boolean.class},
                    scopeCallback(), "local-scope");
            installed += hook(progress, "calculateProgress", float.class,
                    new Class<?>[]{int.class, int.class, int.class},
                    localCalculatorCallback(), "local-calculator");
            installed += hook(gallery, "getGalleryProgress", int.class,
                    new Class<?>[]{boolean.class, function3},
                    galleryBoundaryCallback(), "gallery-boundary");
            installed += hook(gallery, "calculateProgress", int.class,
                    new Class<?>[]{int.class, int.class, int.class, int.class,
                            int.class, int.class, int.class, int.class},
                    galleryCalculatorCallback(), "gallery-calculator");
            installed += hook(progress, "updateScopeUIProgressInfo", void.class,
                    new Class<?>[]{int.class, Bundle.class},
                    outgoingCallback(), "outgoing-bridge");
            installed += hook(activity, "refreshAISearchStatus", void.class,
                    new Class<?>[]{Bundle.class},
                    displayCallback(), "setting-display");
        } catch (Throwable error) {
            XposedBridge.log(TAG + ": global precise setup failed -> " + error);
        }
        chainReady = installed == REQUIRED_HOOKS;
        XposedBridge.log(TAG + ": global precise hooks=" + installed + "/"
                + REQUIRED_HOOKS + " ready=" + chainReady);
        return installed;
    }

    private XC_MethodHook indexCallback() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (enabled()
                            && param.args.length == 3
                            && param.args[0] instanceof Integer scope
                            && param.args[1] instanceof Boolean cache) {
                        param.setObjectExtra("global-index",
                                collector.beginIndex(scope, cache));
                    }
                } catch (Throwable error) {
                    logCallback("index-before", error);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                GlobalProgressRequestCollector.IndexToken token =
                        (GlobalProgressRequestCollector.IndexToken)
                                param.getObjectExtra("global-index");
                if (token == null) {
                    return;
                }
                try {
                    Bundle result = param.getResult() instanceof Bundle bundle
                            ? bundle : null;
                    Integer progress = bundleInteger(result, PROGRESS);
                    long runStart = runningStartTime();
                    long now = SystemClock.elapsedRealtime();
                    Optional<GlobalProgressSnapshot> produced = collector.finishIndex(
                            token, progress, runStart, now
                    );
                    produced.ifPresent(latest::set);
                    if (!enabled() || !chainReady || result == null || progress == null) {
                        return;
                    }
                    GlobalProgressSnapshot snapshot = produced.orElse(latest.get());
                    if (snapshot != null
                            && snapshot.isCompatible(progress, runStart, now)) {
                        GlobalProgressPayload.writeToBundle(result, snapshot);
                        XposedBridge.log(TAG + ": global precise direct="
                                + GlobalProgressDisplay.format(snapshot));
                    }
                } catch (Throwable error) {
                    // Ensure the request stack is cleared even when runtime reflection fails.
                    try {
                        collector.finishIndex(token, null, -1L,
                                SystemClock.elapsedRealtime());
                    } catch (Throwable ignored) {
                    }
                    logCallback("index-after", error);
                }
            }
        };
    }

    private XC_MethodHook migratedCallback() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (enabled()) {
                    collector.markMigratedDirect();
                }
            }
        };
    }

    private XC_MethodHook scopeCallback() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (enabled() && param.args.length > 0
                            && param.args[0] instanceof Integer scope) {
                        param.setObjectExtra("global-scope", collector.beginScope(scope));
                    }
                } catch (Throwable error) {
                    logCallback("scope-before", error);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    collector.finishScope(
                            (GlobalProgressRequestCollector.ScopeToken)
                                    param.getObjectExtra("global-scope"),
                            param.getResult()
                    );
                } catch (Throwable error) {
                    logCallback("scope-after", error);
                }
            }
        };
    }

    private XC_MethodHook localCalculatorCallback() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (enabled()) {
                        collector.captureLocal(param.args);
                    }
                } catch (Throwable error) {
                    logCallback("local-calculator", error);
                }
            }
        };
    }

    private XC_MethodHook galleryBoundaryCallback() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (enabled()) {
                        param.setObjectExtra("global-gallery", collector.beginGallery());
                    }
                } catch (Throwable error) {
                    logCallback("gallery-before", error);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    collector.finishGallery(
                            (GlobalProgressRequestCollector.GalleryToken)
                                    param.getObjectExtra("global-gallery"),
                            param.getResult()
                    );
                } catch (Throwable error) {
                    logCallback("gallery-after", error);
                }
            }
        };
    }

    private XC_MethodHook galleryCalculatorCallback() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (enabled()) {
                        collector.captureGallery(param.args, param.getResult());
                    }
                } catch (Throwable error) {
                    logCallback("gallery-calculator", error);
                }
            }
        };
    }

    private XC_MethodHook outgoingCallback() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (!enabled() || !chainReady || param.args.length != 2
                            || !(param.args[0] instanceof Integer scope)
                            || !(param.args[1] instanceof Bundle bundle)) {
                        return;
                    }
                    Integer progress = bundleInteger(bundle, GLOBAL_PROGRESS);
                    if (progress == null && scope == 31) {
                        progress = bundleInteger(bundle, PROGRESS);
                    }
                    GlobalProgressSnapshot snapshot = latest.get();
                    long runStart = runningStartTime();
                    long now = SystemClock.elapsedRealtime();
                    if (progress != null && snapshot != null
                            && snapshot.isCompatible(progress, runStart, now)) {
                        GlobalProgressPayload.writeToBundle(bundle, snapshot);
                        XposedBridge.log(TAG + ": global precise bridge scope=" + scope
                                + " value=" + GlobalProgressDisplay.format(snapshot));
                    }
                } catch (Throwable error) {
                    logCallback("outgoing", error);
                }
            }
        };
    }

    private XC_MethodHook displayCallback() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (!enabled() || !chainReady || param.args.length != 1
                            || !(param.args[0] instanceof Bundle bundle)) {
                        return;
                    }
                    Optional<GlobalProgressSnapshot> payload =
                            GlobalProgressPayload.readFromBundle(bundle);
                    Integer scope = bundleInteger(bundle, SCOPE);
                    Integer status = bundleInteger(bundle, STATUS);
                    Integer progress = bundleInteger(bundle, PROGRESS);
                    if (payload.isEmpty() || scope == null
                            || status == null || progress == null) {
                        return;
                    }

                    Object binding = XposedHelpers.getObjectField(
                            param.thisObject, "mBinding");
                    Object descriptionObject = XposedHelpers.getObjectField(
                            binding, "tvAiSearchDesc");
                    Object buttonObject = XposedHelpers.getObjectField(
                            binding, "mbtAnalyze");
                    Object buttonTextObject = XposedHelpers.getObjectField(
                            buttonObject, "mTextView");
                    if (!(descriptionObject instanceof TextView description)
                            || !(buttonObject instanceof View button)
                            || !(buttonTextObject instanceof TextView buttonText)) {
                        return;
                    }

                    CharSequence originalDescription = description.getText();
                    CharSequence originalButtonText = buttonText.getText();
                    CharSequence originalContentDescription =
                            button.getContentDescription();
                    Optional<GlobalProgressDisplay.RenderPlan> plan =
                            GlobalProgressDisplay.plan(
                                    originalDescription,
                                    originalButtonText,
                                    originalContentDescription,
                                    scope,
                                    status,
                                    progress,
                                    payload.get(),
                                    runningStartTime(),
                                    SystemClock.elapsedRealtime()
                            );
                    if (plan.isEmpty()) {
                        return;
                    }
                    try {
                        description.setText(plan.get().description());
                        buttonText.setText(plan.get().buttonText());
                        button.setContentDescription(plan.get().contentDescription());
                    } catch (Throwable writeError) {
                        restore(description, originalDescription);
                        restore(buttonText, originalButtonText);
                        restore(button, originalContentDescription);
                        throw writeError;
                    }
                    XposedBridge.log(TAG + ": global precise display="
                            + plan.get().buttonText());
                } catch (Throwable error) {
                    logCallback("display", error);
                }
            }
        };
    }

    private int hook(
            Class<?> owner,
            String name,
            Class<?> returnType,
            Class<?>[] parameters,
            XC_MethodHook callback,
            String id
    ) {
        try {
            Method method = owner.getDeclaredMethod(name, parameters);
            if (!method.getReturnType().equals(returnType)) {
                throw new NoSuchMethodException("Return type mismatch");
            }
            XposedBridge.hookMethod(method, callback);
            XposedBridge.log(TAG + ": global precise exact -> " + id);
            return 1;
        } catch (Throwable error) {
            XposedBridge.log(TAG + ": global precise unavailable " + id
                    + " -> " + error);
            return 0;
        }
    }

    private boolean enabled() {
        return configClient.snapshot().shouldBypass(Policy.AI_UI_CAPABILITY);
    }

    private long runningStartTime() {
        Object result = XposedHelpers.callMethod(runningStatus, "getRunningStartTime");
        return result instanceof Long value ? value : -1L;
    }

    private Class<?> find(String name) {
        return XposedHelpers.findClass(name, classLoader);
    }

    private static Integer bundleInteger(Bundle bundle, String key) {
        if (bundle == null || !bundle.containsKey(key)) {
            return null;
        }
        Object value = bundle.get(key);
        return value instanceof Integer integer ? integer : null;
    }

    private static void restore(TextView view, CharSequence text) {
        try {
            view.setText(text);
        } catch (Throwable ignored) {
        }
    }

    private static void restore(View view, CharSequence description) {
        try {
            view.setContentDescription(description);
        } catch (Throwable ignored) {
        }
    }

    private static void logCallback(String id, Throwable error) {
        XposedBridge.log(TAG + ": global precise " + id + " failed -> " + error);
    }
}

package com.wayne.hyperaicrbypass.hook;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import com.wayne.hyperaicrbypass.config.ConfigClient;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.wayne.hyperaicrbypass.xposed.ModernHook;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;
import com.wayne.hyperaicrbypass.xposed.ReflectionHelpers;

public final class GlobalPreciseProgressHooks {
    private static final String TAG = "HyperAICRBypass";
    private static final String PROGRESS = "analyse_progress";
    private static final String GLOBAL_PROGRESS = "global_analyse_progress";
    private static final String STATUS = "analyse_status";
    private static final String SCOPE = "scope";

    private final Context context;
    private final ClassLoader classLoader;
    private final ConfigClient configClient;
    private final GlobalProgressRequestCollector collector =
            new GlobalProgressRequestCollector();
    private final AtomicReference<GlobalProgressSnapshot> latest =
            new AtomicReference<>();
    private volatile Set<String> installedPointIds = Set.of();
    private Object runningStatus;

    public GlobalPreciseProgressHooks(Context context, ConfigClient configClient) {
        this.context = context;
        this.classLoader = context.getClassLoader();
        this.configClient = configClient;
    }

    public int install() {
        installedPointIds = Set.of();
        Set<String> installed = new LinkedHashSet<>();
        List<GlobalProgressHookCatalog.Point> missing = new ArrayList<>();
        try {
            runningStatus = ReflectionHelpers.getStaticObjectField(
                    find("com.xiaomi.aicr.searchpro.monitor.RunningStatus"), "INSTANCE");
            for (GlobalProgressHookCatalog.Point point
                    : GlobalProgressHookCatalog.points()) {
                if (installExact(point)) {
                    installed.add(point.id());
                } else {
                    missing.add(point);
                }
            }
            if (!missing.isEmpty()) {
                installed.addAll(installSemanticFallbacks(missing));
            }
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": global precise setup failed -> " + error);
        }
        installedPointIds = Set.copyOf(installed);
        int requiredHooks = GlobalProgressHookCatalog.points().size();
        ModernXposed.log(TAG + ": global precise hooks=" + installed.size() + "/"
                + requiredHooks + " direct="
                + ready(GlobalProgressBranch.MIGRATED_DIRECT_AI)
                + " post=" + ready(GlobalProgressBranch.MIGRATED_POSTPROCESSED)
                + " unmigrated=" + ready(GlobalProgressBranch.UNMIGRATED_LOCAL));
        return installed.size();
    }

    private ModernHook indexCallback() {
        return new ModernHook() {
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
                    if (!enabled() || result == null || progress == null) {
                        return;
                    }
                    GlobalProgressSnapshot snapshot = produced.orElse(latest.get());
                    if (snapshot != null
                            && ready(snapshot.branch())
                            && snapshot.isCompatible(progress, runStart, now)) {
                        GlobalProgressPayload.writeToBundle(result, snapshot);
                        ModernXposed.log(TAG + ": global precise direct="
                                + GlobalProgressDisplay.format(
                                        snapshot, configClient.progressPrecision()));
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

    private ModernHook migratedCallback() {
        return new ModernHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (enabled()) {
                    collector.markMigratedDirect();
                }
            }
        };
    }

    private ModernHook unmigratedCallback() {
        return new ModernHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (enabled()) {
                    collector.markUnmigratedLocal();
                }
            }
        };
    }

    private ModernHook scopeCallback() {
        return new ModernHook() {
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

    private ModernHook localCalculatorCallback() {
        return new ModernHook() {
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

    private ModernHook galleryBoundaryCallback() {
        return new ModernHook() {
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

    private ModernHook galleryCalculatorCallback() {
        return new ModernHook() {
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

    private ModernHook galleryPostprocessCallback() {
        return new ModernHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (enabled()) {
                        collector.captureMigrationPostprocess(
                                param.args, param.getResult());
                    }
                } catch (Throwable error) {
                    logCallback("gallery-postprocess", error);
                }
            }
        };
    }

    private ModernHook outgoingCallback() {
        return new ModernHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (!enabled() || param.args.length != 2
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
                            && ready(snapshot.branch())
                            && snapshot.isCompatible(progress, runStart, now)) {
                        GlobalProgressPayload.writeToBundle(bundle, snapshot);
                        ModernXposed.log(TAG + ": global precise bridge scope=" + scope
                                + " value=" + GlobalProgressDisplay.format(
                                        snapshot, configClient.progressPrecision()));
                    }
                } catch (Throwable error) {
                    logCallback("outgoing", error);
                }
            }
        };
    }

    private ModernHook notificationCallback() {
        return new ModernHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (!enabled() || param.args.length != 2
                            || !(param.args[0] instanceof Integer scope)
                            || !(param.args[1] instanceof Boolean forceUpdate)) {
                        return;
                    }
                    boolean migratedReady = ready(
                            GlobalProgressBranch.MIGRATED_DIRECT_AI)
                            || ready(GlobalProgressBranch.MIGRATED_POSTPROCESSED);
                    boolean unmigratedReady = ready(
                            GlobalProgressBranch.UNMIGRATED_LOCAL);
                    if (GlobalProgressHookLogic.shouldForceNotification(
                            migratedReady, unmigratedReady, scope)) {
                        param.args[1] = true;
                        if (!forceUpdate) {
                            ModernXposed.log(TAG
                                    + ": global precise notify forced scope=" + scope);
                        }
                    }
                } catch (Throwable error) {
                    logCallback("notification", error);
                }
            }
        };
    }

    private ModernHook displayCallback() {
        return new ModernHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (!enabled() || param.args.length != 1
                            || !(param.args[0] instanceof Bundle bundle)) {
                        return;
                    }
                    int scope = bundle.getInt(SCOPE, 0);
                    int status = bundle.getInt(STATUS, 0);
                    int progress = bundle.getInt(PROGRESS, -1);
                    int normalized = GlobalProgressHookLogic.normalizeUiStatus(
                            scope,
                            status,
                            progress,
                            bundle.getBoolean("initiative_start", false),
                            bundle.getBoolean("initiative_pause", false),
                            bundle.getBoolean("curr_paused_by_handle", false)
                    );
                    if (normalized != status) {
                        bundle.putInt(STATUS, normalized);
                        ModernXposed.log(TAG + ": global UI transition status "
                                + status + " -> " + normalized);
                    }
                } catch (Throwable error) {
                    logCallback("display-transition", error);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (!enabled() || param.args.length != 1
                            || !(param.args[0] instanceof Bundle bundle)) {
                        return;
                    }
                    Optional<GlobalProgressSnapshot> payload =
                            GlobalProgressPayload.readFromBundle(bundle);
                    Integer scope = bundleInteger(bundle, SCOPE);
                    Integer status = bundleInteger(bundle, STATUS);
                    Integer progress = bundleInteger(bundle, PROGRESS);
                    if (scope == null || status == null || progress == null) {
                        return;
                    }
                    Optional<GlobalProgressDisplay.LoadingPlan> loading =
                            GlobalProgressDisplay.loadingPlan(
                                    scope,
                                    progress,
                                    bundle.getBoolean("initiative_start", false),
                                    bundle.getBoolean("initiative_pause", false),
                                    bundle.getBoolean("curr_paused_by_handle", false)
                            );
                    if (loading.isPresent()) {
                        Object binding = ReflectionHelpers.getObjectField(
                                param.thisObject, "mBinding");
                        Object buttonObject = ReflectionHelpers.getObjectField(
                                binding, "mbtAnalyze");
                        Object buttonTextObject = ReflectionHelpers.getObjectField(
                                buttonObject, "mTextView");
                        if (buttonObject instanceof View button
                                && buttonTextObject instanceof TextView buttonText) {
                            buttonText.setText(loading.get().buttonText());
                            button.setContentDescription(loading.get().buttonText());
                            button.setEnabled(loading.get().enabled());
                            ModernXposed.log(TAG + ": global precise loading");
                        }
                        return;
                    }
                    long runStart = runningStartTime();
                    long now = SystemClock.elapsedRealtime();
                    boolean transition = bundle.getBoolean("status_change", false)
                            || bundle.getBoolean("initiative_start", false)
                            || bundle.getBoolean("initiative_pause", false)
                            || bundle.getBoolean("curr_paused_by_handle", false);
                    Optional<GlobalProgressSnapshot> selected =
                            GlobalProgressHookLogic.displaySnapshot(
                                    payload, latest.get(), progress, runStart, now, transition
                            );
                    if (selected.isEmpty() || !ready(selected.get().branch())) {
                        return;
                    }
                    latest.set(selected.get());

                    Object binding = ReflectionHelpers.getObjectField(
                            param.thisObject, "mBinding");
                    Object descriptionObject = ReflectionHelpers.getObjectField(
                            binding, "tvAiSearchDesc");
                    Object buttonObject = ReflectionHelpers.getObjectField(
                            binding, "mbtAnalyze");
                    Object buttonTextObject = ReflectionHelpers.getObjectField(
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
                                    selected.get(),
                                    runStart,
                                    now,
                                    configClient.progressPrecision()
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
                    ModernXposed.log(TAG + ": global precise display="
                            + plan.get().buttonText());
                } catch (Throwable error) {
                    logCallback("display", error);
                }
            }
        };
    }

    private boolean installExact(GlobalProgressHookCatalog.Point point) {
        try {
            Class<?> owner = find(point.className());
            Method method = owner.getDeclaredMethod(
                    point.methodName(), resolveTypes(point.parameterTypes())
            );
            if (!method.getReturnType().equals(resolveType(point.returnType()))) {
                throw new NoSuchMethodException("Return type mismatch");
            }
            ModernXposed.hookMethod(method, callback(point.id()));
            ModernXposed.log(TAG + ": global precise exact -> " + point.id());
            return true;
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": global precise exact unavailable " + point.id()
                    + " -> " + error);
            return false;
        }
    }

    private Set<String> installSemanticFallbacks(
            List<GlobalProgressHookCatalog.Point> missing
    ) {
        Set<String> installed = new LinkedHashSet<>();
        try (DexKitBridge bridge = DexKitBridge.create(
                context.getApplicationInfo().sourceDir)) {
            for (GlobalProgressHookCatalog.Point point : missing) {
                Method candidate = findUniqueCandidate(bridge, point);
                if (candidate == null) {
                    ModernXposed.log(TAG + ": global precise unavailable " + point.id());
                    continue;
                }
                try {
                    ModernXposed.hookMethod(candidate, callback(point.id()));
                    installed.add(point.id());
                    ModernXposed.log(TAG + ": global precise semantic -> "
                            + descriptor(candidate));
                } catch (Throwable error) {
                    ModernXposed.log(TAG + ": global precise semantic registration failed "
                            + point.id() + " -> " + error);
                }
            }
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": global precise semantic discovery failed -> "
                    + error);
        }
        return installed;
    }

    private Method findUniqueCandidate(
            DexKitBridge bridge,
            GlobalProgressHookCatalog.Point point
    ) {
        try {
            MethodMatcher matcher = MethodMatcher.create()
                    .returnType(point.returnType())
                    .paramTypes(point.parameterTypes());
            if (!point.requiredAnchors().isEmpty()) {
                matcher.usingStrings(point.requiredAnchors());
            }
            FindMethod query = FindMethod.create()
                    .searchPackages(point.packageName())
                    .matcher(matcher);
            Set<Method> candidates = new LinkedHashSet<>();
            for (MethodData data : bridge.findMethod(query)) {
                if (Modifier.isStatic(data.getModifiers())) {
                    continue;
                }
                Method method = data.getMethodInstance(classLoader);
                if (method.getDeclaringClass().getName().equals(point.className())
                        && matchesShape(method, point)) {
                    candidates.add(method);
                }
            }
            return candidates.size() == 1 ? candidates.iterator().next() : null;
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": global precise semantic query failed "
                    + point.id() + " -> " + error);
            return null;
        }
    }

    private ModernHook callback(String id) {
        return switch (id) {
            case "index" -> indexCallback();
            case "migrated" -> migratedCallback();
            case "unmigrated" -> unmigratedCallback();
            case "local-scope" -> scopeCallback();
            case "local-calculator" -> localCalculatorCallback();
            case "gallery-boundary" -> galleryBoundaryCallback();
            case "gallery-calculator" -> galleryCalculatorCallback();
            case "gallery-postprocess" -> galleryPostprocessCallback();
            case "notification" -> notificationCallback();
            case "outgoing-bridge" -> outgoingCallback();
            case "setting-display" -> displayCallback();
            default -> throw new IllegalArgumentException("Unknown global hook " + id);
        };
    }

    private boolean matchesShape(
            Method method,
            GlobalProgressHookCatalog.Point point
    ) {
        if (!method.getReturnType().getName().equals(point.returnType())
                || Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length != point.parameterTypes().size()) {
            return false;
        }
        for (int index = 0; index < parameters.length; index++) {
            if (!parameters[index].getName().equals(point.parameterTypes().get(index))) {
                return false;
            }
        }
        return true;
    }

    private Class<?>[] resolveTypes(List<String> names) throws ClassNotFoundException {
        Class<?>[] types = new Class<?>[names.size()];
        for (int index = 0; index < names.size(); index++) {
            types[index] = resolveType(names.get(index));
        }
        return types;
    }

    private Class<?> resolveType(String name) throws ClassNotFoundException {
        return switch (name) {
            case "boolean" -> boolean.class;
            case "float" -> float.class;
            case "int" -> int.class;
            case "void" -> void.class;
            default -> Class.forName(name, false, classLoader);
        };
    }

    private static String descriptor(Method method) {
        List<String> parameters = new ArrayList<>();
        for (Class<?> type : method.getParameterTypes()) {
            parameters.add(type.getName());
        }
        return method.getDeclaringClass().getName() + "#" + method.getName()
                + "(" + String.join(",", parameters) + ")";
    }

    private boolean enabled() {
        return configClient.progressPrecision().isPrecise();
    }

    private boolean ready(GlobalProgressBranch branch) {
        return installedPointIds.containsAll(
                GlobalProgressHookCatalog.requiredPointIds(branch));
    }

    private long runningStartTime() {
        Object result = ReflectionHelpers.callMethod(runningStatus, "getRunningStartTime");
        return result instanceof Long value ? value : -1L;
    }

    private Class<?> find(String name) {
        return ReflectionHelpers.findClass(name, classLoader);
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
        ModernXposed.log(TAG + ": global precise " + id + " failed -> " + error);
    }
}

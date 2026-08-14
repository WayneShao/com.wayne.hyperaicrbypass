package com.wayne.hyperaicrbypass.hook;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;

import com.wayne.hyperaicrbypass.adapt.SemanticQuerySpec;
import com.wayne.hyperaicrbypass.adapt.DexKitBridgeFactory;
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
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.wayne.hyperaicrbypass.xposed.ModernHook;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;
import com.wayne.hyperaicrbypass.xposed.ReflectionHelpers;

public final class PreciseProgressHooks {
    private static final String TAG = "HyperAICRBypass";
    private static final String PROGRESS_KEY = "analyse_progress";

    private final Context context;
    private final ClassLoader classLoader;
    private final ConfigClient configClient;
    private final AicrRuntimeLayout layout;
    private final AtomicReference<PreciseProgressSnapshot> latest = new AtomicReference<>();
    private final Set<PreciseProgressHookCatalog.Kind> installedKinds =
            java.util.EnumSet.noneOf(PreciseProgressHookCatalog.Kind.class);
    private volatile boolean notificationChainReady;

    public PreciseProgressHooks(Context context, ConfigClient configClient) {
        this(context, configClient, AicrRuntimeLayout.detect(
                AicrPackageVersion.branch(context), context.getClassLoader()));
    }

    public PreciseProgressHooks(
            Context context,
            ConfigClient configClient,
            AicrRuntimeLayout layout
    ) {
        this.context = context;
        this.classLoader = context.getClassLoader();
        this.configClient = configClient;
        this.layout = layout;
    }

    public int install() {
        notificationChainReady = false;
        int installed = 0;
        List<PreciseProgressHookCatalog.Point> missing = new ArrayList<>();
        List<PreciseProgressHookCatalog.Point> points =
                PreciseProgressHookCatalog.points(layout);
        for (PreciseProgressHookCatalog.Point point : points) {
            if (installedKinds.contains(point.kind())) {
                installed++;
                continue;
            }
            if (installExact(point)) {
                installedKinds.add(point.kind());
                installed++;
            } else {
                missing.add(point);
            }
        }
        if (!missing.isEmpty()) {
            installed += installSemanticFallbacks(missing);
        }
        notificationChainReady = installed == points.size();
        ModernXposed.log(TAG + ": precise progress hooks=" + installed + "/"
                + points.size());
        return installed;
    }

    private boolean installExact(PreciseProgressHookCatalog.Point point) {
        try {
            Class<?> owner = ReflectionHelpers.findClass(point.className(), classLoader);
            Method method = owner.getDeclaredMethod(
                    point.methodName(), resolveTypes(point.parameterTypes())
            );
            if (!method.getReturnType().equals(resolveType(point.returnType()))) {
                throw new NoSuchMethodException("Return type mismatch");
            }
            ModernXposed.hookMethod(method, callback(point.kind()));
            ModernXposed.log(TAG + ": precise exact -> " + point.id());
            return true;
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": precise exact unavailable " + point.id()
                    + " -> " + error);
            return false;
        }
    }

    private int installSemanticFallbacks(List<PreciseProgressHookCatalog.Point> points) {
        int installed = 0;
        try (DexKitBridge bridge = DexKitBridgeFactory.create(context)) {
            for (PreciseProgressHookCatalog.Point point : points) {
                Method candidate = findUniqueCandidate(bridge, point);
                if (candidate == null) {
                    candidate = PreciseProgressHookCatalog.branchFallbacks(layout).stream()
                            .filter(fallback -> fallback.kind() == point.kind())
                            .map(fallback -> findUniqueCandidate(bridge, fallback))
                            .filter(java.util.Objects::nonNull)
                            .findFirst().orElse(null);
                }
                if (candidate == null) {
                    ModernXposed.log(TAG + ": precise unavailable " + point.kind());
                    continue;
                }
                try {
                    ModernXposed.hookMethod(candidate, callback(point.kind()));
                    installedKinds.add(point.kind());
                    installed++;
                    ModernXposed.log(TAG + ": precise semantic -> " + descriptor(candidate));
                } catch (Throwable error) {
                    ModernXposed.log(TAG + ": precise semantic registration failed "
                            + point.kind() + " -> " + error);
                }
            }
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": precise semantic discovery unavailable -> " + error);
        }
        return installed;
    }

    private Method findUniqueCandidate(
            DexKitBridge bridge,
            PreciseProgressHookCatalog.Point point
    ) {
        try {
            SemanticQuerySpec spec = point.semanticQuery();
            MethodMatcher matcher = MethodMatcher.create()
                    .returnType(spec.returnType());
            boolean flexibleFunction3 = PreciseProgressHookCatalog
                    .usesAssignableFunction3(layout, point);
            if (!flexibleFunction3) {
                matcher.paramTypes(spec.parameterTypes());
            }
            matcher.usingStrings(spec.requiredAnchors());
            FindMethod query = FindMethod.create().matcher(matcher);
            Set<Method> candidates = new LinkedHashSet<>();
            for (MethodData data : bridge.findMethod(query)) {
                if (Modifier.isStatic(data.getModifiers()) != spec.isStatic()
                        || !SemanticHooks.isExpectedOwner(layout, data.getClassName())) {
                    continue;
                }
                Method method = data.getMethodInstance(classLoader);
                if (SemanticMethodShape.matches(
                        method,
                        spec.returnType(),
                        spec.parameterTypes(),
                        spec.isStatic(),
                        flexibleFunction3
                )) {
                    candidates.add(method);
                }
            }
            return candidates.size() == 1 ? candidates.iterator().next() : null;
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": precise semantic query failed " + point.kind()
                    + " -> " + error);
            return null;
        }
    }

    private ModernHook callback(PreciseProgressHookCatalog.Kind kind) {
        return new ModernHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (kind != PreciseProgressHookCatalog.Kind.NOTIFY) {
                    return;
                }
                try {
                    if (!configClient.progressPrecision().isPrecise()) {
                        return;
                    }
                    forceUiNotification(param);
                } catch (Throwable error) {
                    ModernXposed.log(TAG + ": precise " + kind + " callback failed -> "
                            + error);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (kind == PreciseProgressHookCatalog.Kind.NOTIFY) {
                    return;
                }
                try {
                    if (!configClient.progressPrecision().isPrecise()) {
                        return;
                    }
                    switch (kind) {
                        case CAPTURE -> capture(param);
                        case TRANSPORT -> transport(param);
                        case DISPLAY -> display(param);
                        case NOTIFY -> {
                        }
                    }
                } catch (Throwable error) {
                    ModernXposed.log(TAG + ": precise " + kind + " callback failed -> "
                            + error);
                }
            }
        };
    }

    private void forceUiNotification(ModernHook.MethodHookParam param) {
        if (param.args.length < 2
                || !(param.args[0] instanceof Integer scope)
                || !(param.args[1] instanceof Boolean forceUpdate)
                || !PreciseProgressHookLogic.shouldForceUiNotification(
                        notificationChainReady, scope)) {
            return;
        }
        param.args[1] = true;
        if (!forceUpdate) {
            ModernXposed.log(TAG + ": precise notify forced scope=" + scope);
        }
    }

    private void capture(ModernHook.MethodHookParam param) {
        latest.set(PreciseProgressHookLogic.snapshotFromCalculator(
                param.args, param.getResult(), SystemClock.elapsedRealtime()
        ).orElse(null));
    }

    private void transport(ModernHook.MethodHookParam param) {
        if (param.args.length == 0
                || !(param.args[0] instanceof Integer scope)
                || !(param.getResult() instanceof Bundle result)) {
            return;
        }
        OptionalInt progress = requiredBundleInteger(result, PROGRESS_KEY);
        PreciseProgressSnapshot snapshot = latest.get();
        if (!PreciseProgressHookLogic.shouldAttach(
                true,
                scope,
                progress.isPresent() ? progress.getAsInt() : null,
                snapshot,
                SystemClock.elapsedRealtime()
        )) {
            return;
        }
        PreciseProgressPayload.writeToBundle(result, snapshot);
        ModernXposed.log(TAG + ": precise payload progress=" + progress.getAsInt());
    }

    private void display(ModernHook.MethodHookParam param) {
        Bundle input = null;
        Object activity = param.thisObject;
        for (Object argument : param.args) {
            if (argument instanceof Bundle bundle) {
                input = bundle;
            } else if (argument != null && argument.getClass().getName().equals(
                    "com.xiaomi.aicr.aisearch.progress.AISearchProgressActivity"
            )) {
                activity = argument;
            }
        }
        if (input == null || activity == null) {
            return;
        }
        OptionalInt progress = requiredBundleInteger(input, PROGRESS_KEY);
        Optional<PreciseProgressSnapshot> payload =
                PreciseProgressPayload.readFromBundle(input);
        payload.ifPresent(latest::set);
        Optional<PreciseProgressSnapshot> snapshot =
                PreciseProgressHookLogic.displaySnapshot(payload, latest.get());
        if (progress.isEmpty() || snapshot.isEmpty()) {
            return;
        }
        Object binding = ReflectionHelpers.getObjectField(activity, "mBinding");
        Object status = ReflectionHelpers.getObjectField(binding, "tvBusinessStatus");
        if (!(status instanceof TextView statusView)) {
            return;
        }
        Object scopeValue = ReflectionHelpers.getObjectField(activity, "mScopePkg");
        String scopePackage = scopeValue instanceof String value ? value : null;
        CharSequence original = statusView.getText();
        CharSequence rendered = PreciseProgressDisplay.render(
                original,
                scopePackage,
                progress.getAsInt(),
                snapshot.get(),
                SystemClock.elapsedRealtime(),
                configClient.progressPrecision()
        );
        if (rendered != null && !rendered.toString().contentEquals(original)) {
            statusView.setText(rendered);
            ModernXposed.log(TAG + ": precise display="
                    + PreciseProgressDisplay.format(
                            snapshot.get(), configClient.progressPrecision()));
        }
    }

    private OptionalInt requiredBundleInteger(Bundle bundle, String key) {
        if (!bundle.containsKey(key)) {
            return OptionalInt.empty();
        }
        Object value = bundle.get(key);
        return value instanceof Integer integer
                ? OptionalInt.of(integer)
                : OptionalInt.empty();
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
            case "int" -> int.class;
            case "long" -> long.class;
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
}

package com.wayne.hyperaicrbypass.hook;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;

import com.wayne.hyperaicrbypass.adapt.SemanticQuerySpec;
import com.wayne.hyperaicrbypass.config.ConfigClient;
import com.wayne.hyperaicrbypass.config.Policy;

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
    private final AtomicReference<PreciseProgressSnapshot> latest = new AtomicReference<>();
    private volatile boolean notificationChainReady;

    public PreciseProgressHooks(Context context, ConfigClient configClient) {
        this.context = context;
        this.classLoader = context.getClassLoader();
        this.configClient = configClient;
    }

    public int install() {
        notificationChainReady = false;
        int installed = 0;
        List<PreciseProgressHookCatalog.Point> missing = new ArrayList<>();
        for (PreciseProgressHookCatalog.Point point : PreciseProgressHookCatalog.points()) {
            if (installExact(point)) {
                installed++;
            } else {
                missing.add(point);
            }
        }
        if (!missing.isEmpty()) {
            installed += installSemanticFallbacks(missing);
        }
        notificationChainReady = installed == PreciseProgressHookCatalog.points().size();
        ModernXposed.log(TAG + ": precise progress hooks=" + installed + "/"
                + PreciseProgressHookCatalog.points().size());
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
        try (DexKitBridge bridge = DexKitBridge.create(context.getApplicationInfo().sourceDir)) {
            for (PreciseProgressHookCatalog.Point point : points) {
                Method candidate = findUniqueCandidate(bridge, point);
                if (candidate == null) {
                    ModernXposed.log(TAG + ": precise unavailable " + point.kind());
                    continue;
                }
                try {
                    ModernXposed.hookMethod(candidate, callback(point.kind()));
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
                    .returnType(spec.returnType())
                    .paramTypes(spec.parameterTypes())
                    .usingStrings(spec.requiredAnchors());
            FindMethod query = FindMethod.create()
                    .searchPackages(spec.packagePrefix())
                    .matcher(matcher);
            Set<Method> candidates = new LinkedHashSet<>();
            for (MethodData data : bridge.findMethod(query)) {
                if (Modifier.isStatic(data.getModifiers()) != spec.isStatic()) {
                    continue;
                }
                Method method = data.getMethodInstance(classLoader);
                if (matchesShape(method, spec)) {
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
                    if (!configClient.snapshot().shouldBypass(Policy.AI_UI_CAPABILITY)) {
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
                    if (!configClient.snapshot().shouldBypass(Policy.AI_UI_CAPABILITY)) {
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
                        notificationChainReady,
                        scope,
                        latest.get(),
                        SystemClock.elapsedRealtime())) {
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
        if (param.args.length == 0 || !(param.args[0] instanceof Bundle input)) {
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
        Object binding = ReflectionHelpers.getObjectField(param.thisObject, "mBinding");
        Object status = ReflectionHelpers.getObjectField(binding, "tvBusinessStatus");
        if (!(status instanceof TextView statusView)) {
            return;
        }
        Object scopeValue = ReflectionHelpers.getObjectField(param.thisObject, "mScopePkg");
        String scopePackage = scopeValue instanceof String value ? value : null;
        CharSequence original = statusView.getText();
        CharSequence rendered = PreciseProgressDisplay.render(
                original,
                scopePackage,
                progress.getAsInt(),
                snapshot.get(),
                SystemClock.elapsedRealtime()
        );
        if (rendered != null && !rendered.toString().contentEquals(original)) {
            statusView.setText(rendered);
            ModernXposed.log(TAG + ": precise display="
                    + PreciseProgressDisplay.format(snapshot.get()));
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

    private boolean matchesShape(Method method, SemanticQuerySpec spec) {
        if (!method.getReturnType().getName().equals(spec.returnType())
                || Modifier.isStatic(method.getModifiers()) != spec.isStatic()) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != spec.parameterTypes().size()) {
            return false;
        }
        for (int index = 0; index < parameterTypes.length; index++) {
            if (!parameterTypes[index].getName().equals(spec.parameterTypes().get(index))) {
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

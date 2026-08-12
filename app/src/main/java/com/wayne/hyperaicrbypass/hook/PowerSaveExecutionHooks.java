package com.wayne.hyperaicrbypass.hook;

import android.content.Context;

import com.wayne.hyperaicrbypass.config.ConfigClient;
import com.wayne.hyperaicrbypass.adapt.DexKitBridgeFactory;
import com.wayne.hyperaicrbypass.xposed.ModernHook;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;
import com.wayne.hyperaicrbypass.xposed.ReflectionHelpers;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class PowerSaveExecutionHooks {
    private static final String TAG = "HyperAICRBypass";

    private final Context context;
    private final ClassLoader classLoader;
    private final ConfigClient configClient;
    private final Set<String> installedMethods = new HashSet<>();

    public PowerSaveExecutionHooks(Context context, ConfigClient configClient) {
        this.context = context;
        this.classLoader = context.getClassLoader();
        this.configClient = configClient;
    }

    public synchronized InstallResult install() {
        AicrVersionBranch branch = AicrPackageVersion.branch(context);
        Set<String> covered = new HashSet<>();
        List<PowerSaveHookSpec> missing = new ArrayList<>();
        for (PowerSaveHookSpec spec : PowerSaveHookSpec.catalog(branch)) {
            if (installExact(spec)) {
                covered.add(spec.boundary().name());
            } else {
                missing.add(spec);
            }
        }

        if (!missing.isEmpty()) {
            try (DexKitBridge bridge = DexKitBridgeFactory.create(context)) {
                for (PowerSaveHookSpec spec : missing) {
                    if (installSemantic(bridge, spec)) {
                        covered.add(spec.boundary().name());
                    }
                }
            } catch (Throwable error) {
                ModernXposed.log(TAG + ": power-save discovery unavailable -> " + error);
            }
        }

        InstallResult result = new InstallResult(
                covered.contains(PowerSaveHookSpec.Boundary.START.name()),
                covered.contains(PowerSaveHookSpec.Boundary.STOP.name())
        );
        ModernXposed.log(TAG + ": power-save execution hooks="
                + result.installedCount() + "/2");
        return result;
    }

    static boolean decide(boolean shouldPause, PowerSaveHookSpec spec, boolean original) {
        return shouldPause ? spec.pauseResult() : original;
    }

    static boolean matchesStaticShape(PowerSaveHookSpec spec, boolean isStatic) {
        return spec.allowStatic() == isStatic;
    }

    private static AicrVersionBranch branchFor(PowerSaveHookSpec spec) {
        return spec.className().contains(".") ? AicrVersionBranch.V4 : AicrVersionBranch.V3;
    }

    private boolean installExact(PowerSaveHookSpec spec) {
        try {
            Class<?> owner = ReflectionHelpers.findClass(spec.className(), classLoader);
            Method method = owner.getDeclaredMethod(
                    spec.methodName(), resolveTypes(spec.parameterTypes())
            );
            if (method.getReturnType() != boolean.class
                    || !matchesStaticShape(spec, Modifier.isStatic(method.getModifiers()))) {
                return false;
            }
            return installMethod(method, spec, "exact");
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": power-save exact unavailable "
                    + spec.methodName() + " -> " + error);
            return false;
        }
    }

    private boolean installSemantic(DexKitBridge bridge, PowerSaveHookSpec spec) {
        try {
            MethodMatcher matcher = MethodMatcher.create()
                    .returnType(spec.returnType())
                    .paramTypes(spec.parameterTypes())
                    .usingStrings(spec.requiredAnchors());
            List<MethodData> candidates = new ArrayList<>(bridge.findMethod(
                    FindMethod.create()
                            .matcher(matcher)
            ));
            candidates.removeIf(candidate ->
                    !SemanticHooks.isExpectedOwner(branchFor(spec), candidate.getClassName())
                            || !matchesStaticShape(
                            spec, Modifier.isStatic(candidate.getModifiers())
                    ));
            MethodData selected = selectUnambiguous(spec, candidates);
            if (selected == null) {
                ModernXposed.log(TAG + ": power-save semantic ambiguous "
                        + spec.methodName() + " candidates=" + candidates.size());
                return false;
            }
            Method method = selected.getMethodInstance(classLoader);
            if (method.getReturnType() != boolean.class
                    || !parameterNames(method).equals(spec.parameterTypes())
                    || !matchesStaticShape(spec, Modifier.isStatic(method.getModifiers()))) {
                return false;
            }
            return installMethod(method, spec, "semantic");
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": power-save semantic failed "
                    + spec.methodName() + " -> " + error);
            return false;
        }
    }

    private boolean installMethod(Method method, PowerSaveHookSpec spec, String source) {
        String descriptor = descriptor(method);
        if (installedMethods.contains(descriptor)) {
            return true;
        }
        try {
            ModernXposed.hookMethod(method, callback(spec));
            installedMethods.add(descriptor);
            ModernXposed.log(TAG + ": power-save " + source + " -> " + descriptor);
            return true;
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": power-save registration failed "
                    + descriptor + " -> " + error);
            return false;
        }
    }

    private ModernHook callback(PowerSaveHookSpec spec) {
        return new ModernHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (configClient.shouldPause()) {
                    param.setResult(spec.pauseResult());
                }
            }
        };
    }

    private Class<?>[] resolveTypes(List<String> names) throws ClassNotFoundException {
        Class<?>[] result = new Class<?>[names.size()];
        for (int i = 0; i < names.size(); i++) {
            result[i] = switch (names.get(i)) {
                case "int" -> int.class;
                case "boolean" -> boolean.class;
                default -> Class.forName(names.get(i), false, classLoader);
            };
        }
        return result;
    }

    private static MethodData selectUnambiguous(
            PowerSaveHookSpec spec,
            List<MethodData> candidates
    ) {
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        List<MethodData> named = candidates.stream()
                .filter(candidate -> candidate.getMethodName().equals(spec.methodName()))
                .collect(Collectors.toList());
        return named.size() == 1 ? named.get(0) : null;
    }

    private static List<String> parameterNames(Method method) {
        List<String> names = new ArrayList<>();
        for (Class<?> type : method.getParameterTypes()) {
            names.add(type.getName());
        }
        return names;
    }

    private static String descriptor(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName()
                + "(" + String.join(",", parameterNames(method)) + ")";
    }

    public record InstallResult(boolean startInstalled, boolean needStopInstalled) {
        public int installedCount() {
            return (startInstalled ? 1 : 0) + (needStopInstalled ? 1 : 0);
        }

        public boolean complete() {
            return startInstalled && needStopInstalled;
        }
    }
}

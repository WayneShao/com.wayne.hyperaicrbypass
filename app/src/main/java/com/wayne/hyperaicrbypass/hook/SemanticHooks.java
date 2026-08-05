package com.wayne.hyperaicrbypass.hook;

import android.content.Context;

import com.wayne.hyperaicrbypass.adapt.SemanticHookCatalog;
import com.wayne.hyperaicrbypass.adapt.SemanticHookSpec;
import com.wayne.hyperaicrbypass.config.ConfigClient;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

import com.wayne.hyperaicrbypass.xposed.ModernHook;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;

public final class SemanticHooks {
    private static final String TAG = "HyperAICRBypass";

    private final Context context;
    private final ClassLoader classLoader;
    private final ConfigClient configClient;
    private final Set<String> installedIds;
    private final EnumMap<com.wayne.hyperaicrbypass.config.Policy, Integer> policyCounts =
            new EnumMap<>(com.wayne.hyperaicrbypass.config.Policy.class);

    public SemanticHooks(
            Context context,
            ConfigClient configClient,
            Set<String> registeredIds
    ) {
        this.context = context;
        this.classLoader = context.getClassLoader();
        this.configClient = configClient;
        this.installedIds = new HashSet<>(registeredIds);
    }

    public synchronized int install(List<HookSpec> missingExact) {
        if (!SemanticDiscoveryPolicy.needsDiscovery(missingExact)) {
            return 0;
        }
        int successes = 0;
        try (DexKitBridge bridge = DexKitBridge.create(context.getApplicationInfo().sourceDir)) {
            for (HookSpec missing : missingExact) {
                if (!missing.className().startsWith("com.xiaomi.aicr.")) {
                    continue;
                }
                SemanticHookSpec semantic = matchingSpec(missing);
                if (semantic == null) {
                    continue;
                }
                for (Method method : findCandidates(bridge, semantic)) {
                    String id = descriptor(method);
                    if (!installedIds.add(id)) {
                        continue;
                    }
                    try {
                        ModernXposed.hookMethod(method, callback(semantic));
                        successes++;
                        policyCounts.merge(semantic.policy(), 1, Integer::sum);
                        ModernXposed.log(TAG + ": semantic " + semantic.policy().getKey()
                                + " -> " + id);
                    } catch (Throwable error) {
                        installedIds.remove(id);
                        ModernXposed.log(TAG + ": semantic registration failed " + id
                                + " -> " + error);
                    }
                }
            }
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": semantic discovery unavailable -> " + error);
        }
        return successes;
    }

    public synchronized Map<com.wayne.hyperaicrbypass.config.Policy, Integer> policyCounts() {
        return Map.copyOf(policyCounts);
    }

    private List<Method> findCandidates(DexKitBridge bridge, SemanticHookSpec spec) {
        try {
            MethodMatcher matcher = MethodMatcher.create()
                    .returnType(spec.returnType())
                    .paramTypes(spec.parameterTypes())
                    .usingStrings(spec.requiredAnchors());
            FindMethod query = FindMethod.create()
                    .searchPackages("com.xiaomi.aicr")
                    .matcher(matcher);
            List<MethodData> data = new ArrayList<>(bridge.findMethod(query));
            List<MethodData> shaped = data.stream()
                    .filter(candidate -> Modifier.isStatic(candidate.getModifiers()) == spec.isStatic())
                    .collect(Collectors.toList());
            List<MethodData> selected = selectUnambiguous(spec, shaped);
            LinkedHashSet<Method> methods = new LinkedHashSet<>();
            for (MethodData candidate : selected) {
                Method method = candidate.getMethodInstance(classLoader);
                if (method.getReturnType().getName().equals(spec.returnType())
                        && parameterNames(method).equals(spec.parameterTypes())
                        && Modifier.isStatic(method.getModifiers()) == spec.isStatic()) {
                    methods.add(method);
                }
            }
            return List.copyOf(methods);
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": semantic query failed " + spec.policy().getKey()
                    + "/" + spec.preferredMethodName() + " -> " + error);
            return List.of();
        }
    }

    private static List<MethodData> selectUnambiguous(
            SemanticHookSpec spec,
            List<MethodData> candidates
    ) {
        if (candidates.size() <= 1) {
            return candidates;
        }
        List<MethodData> named = candidates.stream()
                .filter(candidate -> candidate.getMethodName().equals(spec.preferredMethodName()))
                .collect(Collectors.toList());
        if (named.size() == 1) {
            return named;
        }
        if (spec.policy() == com.wayne.hyperaicrbypass.config.Policy.DURATION
                && !named.isEmpty()) {
            return named;
        }
        return List.of();
    }

    private ModernHook callback(SemanticHookSpec spec) {
        return new ModernHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!configClient.snapshot().shouldBypass(spec.policy())) {
                    return;
                }
                switch (spec.behavior()) {
                    case RESULT_FALSE -> param.setResult(false);
                    case RESULT_ZERO_INT -> param.setResult(0);
                    case RESULT_ONE_INT -> param.setResult(1);
                    case RESULT_HUNDRED_INT -> param.setResult(100);
                    case RESULT_ZERO_LONG -> param.setResult(0L);
                    case ARGUMENT_NOW_LONG -> param.args[0] = System.currentTimeMillis();
                    case ARGUMENT_ZERO_BOOLEAN -> param.args[0] = false;
                }
            }
        };
    }

    private static SemanticHookSpec matchingSpec(HookSpec missing) {
        return SemanticHookCatalog.specs().stream()
                .filter(spec -> spec.policy() == missing.policy())
                .filter(spec -> spec.preferredMethodName().equals(missing.methodName()))
                .filter(spec -> spec.returnType().equals(missing.returnType()))
                .filter(spec -> spec.parameterTypes().equals(missing.parameterTypes()))
                .findFirst()
                .orElse(null);
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
}

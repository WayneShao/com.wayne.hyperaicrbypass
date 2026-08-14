package com.wayne.hyperaicrbypass.hook;

import android.content.Context;

import com.wayne.hyperaicrbypass.adapt.SemanticHookCatalog;
import com.wayne.hyperaicrbypass.adapt.SemanticHookSpec;
import com.wayne.hyperaicrbypass.adapt.DexKitBridgeFactory;
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
    private final AicrVersionBranch branch;
    private final AicrRuntimeLayout layout;
    private final Set<String> installedIds;
    private final EnumMap<com.wayne.hyperaicrbypass.config.Policy, Integer> policyCounts =
            new EnumMap<>(com.wayne.hyperaicrbypass.config.Policy.class);

    public SemanticHooks(
            Context context,
            ConfigClient configClient,
            Set<String> registeredIds,
            AicrVersionBranch branch
    ) {
        this(context, configClient, registeredIds, switch (branch) {
            case V3 -> AicrRuntimeLayout.V3_OBFUSCATED;
            case V4 -> AicrRuntimeLayout.V4_READABLE;
            case UNKNOWN -> AicrRuntimeLayout.UNKNOWN;
        });
    }

    public SemanticHooks(
            Context context,
            ConfigClient configClient,
            Set<String> registeredIds,
            AicrRuntimeLayout layout
    ) {
        this.context = context;
        this.classLoader = context.getClassLoader();
        this.configClient = configClient;
        this.installedIds = new HashSet<>(registeredIds);
        this.layout = layout;
        this.branch = layout.branch();
    }

    public synchronized int install(List<HookSpec> missingExact) {
        if (branch != AicrVersionBranch.UNKNOWN
                && !SemanticDiscoveryPolicy.needsDiscovery(missingExact)) {
            return 0;
        }
        int successes = 0;
        try (DexKitBridge bridge = DexKitBridgeFactory.create(context)) {
            for (SemanticHookSpec semantic : semanticSpecsFor(missingExact, layout)) {
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
            FindMethod query = FindMethod.create().matcher(matcher);
            List<MethodData> data = new ArrayList<>(bridge.findMethod(query));
            List<MethodData> shaped = data.stream()
                    .filter(candidate -> Modifier.isStatic(candidate.getModifiers()) == spec.isStatic())
                    .filter(candidate -> isExpectedOwner(layout, candidate.getClassName()))
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

    static boolean isExpectedOwner(AicrVersionBranch branch, String className) {
        if (className == null) {
            return false;
        }
        return switch (branch) {
            case V3 -> !className.contains(".")
                    || className.startsWith("com.xiaomi.aicr.aisearch.");
            case V4 -> className.startsWith("com.xiaomi.aicr.");
            case UNKNOWN -> !className.contains(".")
                    || className.startsWith("com.xiaomi.aicr.");
        };
    }

    static boolean isExpectedOwner(AicrRuntimeLayout layout, String className) {
        if (className == null) {
            return false;
        }
        return switch (layout) {
            case V3_OBFUSCATED -> !className.contains(".")
                    || className.startsWith("com.xiaomi.aicr.aisearch.");
            case V4_READABLE -> className.startsWith("com.xiaomi.aicr.");
            case V4_COMPACT -> !className.contains(".")
                    || className.startsWith("com.xiaomi.aicr.");
            case UNKNOWN -> className.startsWith("com.xiaomi.aicr.");
        };
    }

    private ModernHook callback(SemanticHookSpec spec) {
        return new ModernHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!configClient.shouldBypass(spec.policy())) {
                    return;
                }
                switch (spec.behavior()) {
                    case RESULT_TRUE -> param.setResult(true);
                    case RESULT_FALSE -> param.setResult(false);
                    case RESULT_ZERO_INT -> param.setResult(0);
                    case RESULT_ONE_INT -> param.setResult(1);
                    case RESULT_HUNDRED_INT -> param.setResult(100);
                    case RESULT_ZERO_LONG -> param.setResult(0L);
                    case ARGUMENT_NOW_LONG -> param.args[0] = System.currentTimeMillis();
                    case ARGUMENT_LAST_NOW_LONG ->
                            param.args[param.args.length - 1] = System.currentTimeMillis();
                    case ARGUMENT_ZERO_BOOLEAN -> param.args[0] = false;
                }
            }
        };
    }

    static List<SemanticHookSpec> semanticSpecsFor(List<HookSpec> missingExact) {
        return semanticSpecsFor(missingExact, AicrVersionBranch.V4);
    }

    static List<SemanticHookSpec> semanticSpecsFor(
            List<HookSpec> missingExact,
            AicrVersionBranch branch
    ) {
        if (branch == AicrVersionBranch.UNKNOWN) {
            LinkedHashSet<SemanticHookSpec> all = new LinkedHashSet<>();
            all.addAll(SemanticHookCatalog.specs(AicrVersionBranch.V3));
            all.addAll(SemanticHookCatalog.specs(AicrVersionBranch.V4));
            return List.copyOf(all);
        }
        Set<com.wayne.hyperaicrbypass.config.Policy> missingPolicies = missingExact.stream()
                .map(HookSpec::policy)
                .filter(policy -> policy
                        != com.wayne.hyperaicrbypass.config.Policy.TASK_CONSTRAINTS)
                .collect(Collectors.toSet());
        return SemanticHookCatalog.specs(branch).stream()
                .filter(spec -> missingPolicies.contains(spec.policy()))
                .collect(Collectors.toList());
    }

    static List<SemanticHookSpec> semanticSpecsFor(
            List<HookSpec> missingExact,
            AicrRuntimeLayout layout
    ) {
        if (layout == AicrRuntimeLayout.UNKNOWN) {
            return List.of();
        }
        Set<com.wayne.hyperaicrbypass.config.Policy> missingPolicies = missingExact.stream()
                .map(HookSpec::policy)
                .filter(policy -> policy
                        != com.wayne.hyperaicrbypass.config.Policy.TASK_CONSTRAINTS)
                .collect(Collectors.toSet());
        return SemanticHookCatalog.specs(layout).stream()
                .filter(spec -> missingPolicies.contains(spec.policy()))
                .collect(Collectors.toList());
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

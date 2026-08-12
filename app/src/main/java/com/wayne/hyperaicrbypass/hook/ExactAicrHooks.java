package com.wayne.hyperaicrbypass.hook;

import com.wayne.hyperaicrbypass.config.ConfigClient;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.wayne.hyperaicrbypass.xposed.ModernHook;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;
import com.wayne.hyperaicrbypass.xposed.ReflectionHelpers;

public final class ExactAicrHooks {
    private static final String TAG = "HyperAICRBypass";

    private final ClassLoader classLoader;
    private final ConfigClient configClient;
    private final List<HookSpec> specs;
    private final RegistrationPlanner planner = new RegistrationPlanner();
    private final Set<String> successfulPolicies = new HashSet<>();

    public ExactAicrHooks(
            ClassLoader classLoader,
            ConfigClient configClient,
            AicrVersionBranch branch
    ) {
        this.classLoader = classLoader;
        this.configClient = configClient;
        this.specs = ExactHookCatalog.aicrSpecs(branch);
    }

    public InstallResult install() {
        List<HookSpec> missing = new ArrayList<>();
        for (HookSpec spec : specs) {
            if (!install(spec)) {
                missing.add(spec);
            }
        }
        ModernXposed.log(TAG + ": exact hooks=" + planner.registeredCount()
                + ", covered policies=" + successfulPolicies.size());
        long aicrHookCount = specs.stream()
                .filter(spec -> !spec.className().startsWith("android."))
                .filter(planner::isRegistered)
                .count();
        Set<String> registeredIds = new HashSet<>();
        EnumMap<com.wayne.hyperaicrbypass.config.Policy, Integer> policyCounts =
                new EnumMap<>(com.wayne.hyperaicrbypass.config.Policy.class);
        for (HookSpec spec : specs) {
            if (planner.isRegistered(spec)) {
                registeredIds.add(spec.id());
                policyCounts.merge(spec.policy(), 1, Integer::sum);
            }
        }
        return new InstallResult(missing, registeredIds, policyCounts, (int) aicrHookCount);
    }

    private boolean install(HookSpec spec) {
        if (!planner.shouldAttempt(spec)) {
            return planner.isRegistered(spec);
        }
        try {
            Class<?> owner = ReflectionHelpers.findClass(spec.className(), classLoader);
            Class<?>[] parameters = new Class<?>[spec.parameterTypes().size()];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = resolveType(spec.parameterTypes().get(i));
            }
            Method method = owner.getDeclaredMethod(spec.methodName(), parameters);
            if (!method.getReturnType().equals(resolveType(spec.returnType()))) {
                throw new NoSuchMethodException("Return type mismatch for " + spec.id());
            }
            ModernXposed.hookMethod(method, callback(spec));
            planner.recordSuccess(spec);
            successfulPolicies.add(spec.policy().getKey());
            ModernXposed.log(TAG + ": exact " + spec.policy().getKey() + " -> " + spec.id());
            return true;
        } catch (Throwable error) {
            planner.recordFailure(spec, error.toString());
            ModernXposed.log(TAG + ": exact unavailable " + spec.id() + " -> " + error);
            return false;
        }
    }

    private ModernHook callback(HookSpec spec) {
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
                    case ARGUMENT_ZERO_BOOLEAN -> param.args[0] = false;
                    case ARGUMENT_NOW_LONG -> param.args[0] = System.currentTimeMillis();
                }
            }
        };
    }

    private Class<?> resolveType(String type) throws ClassNotFoundException {
        return switch (type) {
            case "boolean" -> boolean.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "void" -> void.class;
            default -> Class.forName(type, false, classLoader);
        };
    }

    public record InstallResult(
            List<HookSpec> missingSpecs,
            Set<String> registeredIds,
            Map<com.wayne.hyperaicrbypass.config.Policy, Integer> policyCounts,
            int aicrHookCount
    ) {
        public InstallResult {
            missingSpecs = List.copyOf(missingSpecs);
            registeredIds = Set.copyOf(registeredIds);
            policyCounts = Map.copyOf(policyCounts);
        }
    }
}

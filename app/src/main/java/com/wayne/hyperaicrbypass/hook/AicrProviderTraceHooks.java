package com.wayne.hyperaicrbypass.hook;

import android.os.Binder;
import android.os.Bundle;
import android.content.Context;

import com.wayne.hyperaicrbypass.config.ConfigClient;
import com.wayne.hyperaicrbypass.config.Policy;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.wayne.hyperaicrbypass.xposed.ModernHook;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;
import com.wayne.hyperaicrbypass.xposed.ReflectionHelpers;

public final class AicrProviderTraceHooks {
    private static final String TAG = "HyperAICRBypass";
    private static final String NLS_PROVIDER =
            "com.xiaomi.aicr.aisearch.provider.NLSCapabilityProvider";
    private static final List<String> FIELDS = List.of(
            "scope",
            "register_ui_listener",
            "use_cache",
            "only_runState",
            "analyse_progress",
            "analyse_status",
            "global_analyse_progress",
            "initiative_start",
            "initiative_pause",
            "status_change",
            "force_refresh",
            "ui_scopes",
            "has_global_ui_scope",
            "is_run_algo",
            "progress",
            "in_progress",
            "is_support_ai_search_progress",
            "Status"
    );

    private final Context context;
    private final ClassLoader classLoader;
    private final ConfigClient configClient;
    private final Set<String> installedMethods = new HashSet<>();

    public AicrProviderTraceHooks(Context context, ConfigClient configClient) {
        this.context = context;
        this.classLoader = context.getClassLoader();
        this.configClient = configClient;
    }

    public synchronized InstallResult install() {
        EnumMap<AicrProviderHookSpec.Role, Boolean> covered =
                new EnumMap<>(AicrProviderHookSpec.Role.class);
        List<AicrProviderHookSpec> missing = new ArrayList<>();
        for (AicrProviderHookSpec spec : AicrProviderHookSpec.criticalCatalog()) {
            if (installExact(spec)) {
                covered.put(spec.role(), true);
            } else {
                missing.add(spec);
            }
        }
        if (!missing.isEmpty()) {
            try (DexKitBridge bridge = DexKitBridge.create(
                    context.getApplicationInfo().sourceDir
            )) {
                for (AicrProviderHookSpec spec : missing) {
                    if (installSemantic(bridge, spec)) {
                        covered.put(spec.role(), true);
                    }
                }
            } catch (Throwable error) {
                ModernXposed.log(TAG + ": provider discovery unavailable -> " + error);
            }
        }
        boolean nlsInstalled = installExact(
                NLS_PROVIDER, AicrProviderHookSpec.Role.NLS, "NLSCapabilityProvider"
        );
        InstallResult result = new InstallResult(
                installedMethods.size(),
                nlsInstalled,
                covered.getOrDefault(AicrProviderHookSpec.Role.DATABASE, false),
                covered.getOrDefault(AicrProviderHookSpec.Role.UI, false)
        );
        ModernXposed.log(TAG + ": AICR provider hooks=" + result.installedCount()
                + " database=" + result.databaseStartGateInstalled()
                + " ui=" + result.uiStartGateInstalled());
        return result;
    }

    private boolean installExact(AicrProviderHookSpec spec) {
        return installExact(spec.className(), spec.role(), simpleName(spec.className()));
    }

    private boolean installExact(
            String providerClass,
            AicrProviderHookSpec.Role role,
            String label
    ) {
        try {
            Class<?> owner = ReflectionHelpers.findClass(providerClass, classLoader);
            Method method = owner.getDeclaredMethod(
                    "call", String.class, String.class, Bundle.class
            );
            return installMethod(method, role, label, "exact");
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": provider trace unavailable " + providerClass
                    + " -> " + error);
            return false;
        }
    }

    private boolean installSemantic(DexKitBridge bridge, AicrProviderHookSpec spec) {
        try {
            MethodMatcher matcher = MethodMatcher.create()
                    .returnType(spec.returnType())
                    .paramTypes(spec.parameterTypes())
                    .usingStrings(spec.requiredAnchors());
            List<MethodData> candidates = new ArrayList<>(bridge.findMethod(
                    FindMethod.create().searchPackages("com.xiaomi.aicr").matcher(matcher)
            ));
            candidates = candidates.stream()
                    .filter(candidate -> !Modifier.isStatic(candidate.getModifiers()))
                    .collect(Collectors.toList());
            MethodData selected = selectUnambiguous(candidates);
            if (selected == null) {
                ModernXposed.log(TAG + ": provider semantic ambiguous " + spec.role()
                        + " candidates=" + candidates.size());
                return false;
            }
            Method method = selected.getMethodInstance(classLoader);
            if (!method.getReturnType().getName().equals(spec.returnType())
                    || !parameterNames(method).equals(spec.parameterTypes())
                    || Modifier.isStatic(method.getModifiers())) {
                return false;
            }
            return installMethod(
                    method, spec.role(), simpleName(method.getDeclaringClass().getName()),
                    "semantic"
            );
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": provider semantic failed " + spec.role()
                    + " -> " + error);
            return false;
        }
    }

    private boolean installMethod(
            Method method,
            AicrProviderHookSpec.Role role,
            String label,
            String source
    ) {
        String descriptor = descriptor(method);
        if (installedMethods.contains(descriptor)) {
            return true;
        }
        try {
            ModernXposed.hookMethod(method, callback(role, label));
            installedMethods.add(descriptor);
            ModernXposed.log(TAG + ": provider " + source + " " + role
                    + " -> " + descriptor);
            return true;
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": provider registration failed " + descriptor
                    + " -> " + error);
            return false;
        }
    }

    private ModernHook callback(AicrProviderHookSpec.Role role, String providerName) {
        return new ModernHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String method = param.args[0] instanceof String value ? value : null;
                Bundle extras = param.args[2] instanceof Bundle bundle ? bundle : null;
                Action action = decide(
                        role,
                        method,
                        extras != null && extras.getBoolean("is_run_algo", false),
                        configClient.shouldPause()
                );
                if (action == Action.BLOCK_DATABASE_START) {
                    ModernXposed.log(TAG + ": power-save blocked provider start " + method);
                    param.setResult(extras == null ? new Bundle() : extras);
                    return;
                }
                if (action == Action.CONVERT_UI_START_TO_PAUSE && extras != null) {
                    extras.putBoolean("is_run_algo", false);
                    ModernXposed.log(TAG + ": power-save converted UI start to pause");
                }
                boolean preciseEnabled = configClient.progressPrecision().isPrecise();
                if (role == AicrProviderHookSpec.Role.UI
                        && extras != null
                        && preciseEnabled
                        && AicrProgressRequestPolicy.shouldDiscardUiCache(
                                method, extras.getInt("scope", 0))) {
                    int scope = extras.getInt("scope", 0);
                    try {
                        Object cacheObject = ReflectionHelpers.getObjectField(
                                param.thisObject, "mAlgoStateMap");
                        if (cacheObject instanceof Map<?, ?> cache
                                && cache.remove(Integer.valueOf(scope)) != null) {
                            ModernXposed.log(TAG
                                    + ": precise discarded UI cache scope=" + scope);
                        }
                        extras.putBoolean("use_cache", false);
                    } catch (Throwable error) {
                        ModernXposed.log(TAG + ": precise UI cache bypass unavailable -> "
                                + error);
                    }
                }
                if (role == AicrProviderHookSpec.Role.DATABASE
                        && extras != null
                        && preciseEnabled
                        && AicrProgressRequestPolicy.shouldForceLive(
                                method,
                                extras.getInt("scope", 0),
                                extras.getBoolean("register_ui_listener", false),
                                extras.getBoolean("use_cache", false))) {
                    extras.putBoolean("use_cache", false);
                    ModernXposed.log(TAG + ": precise first response live scope="
                            + extras.getInt("scope", 0));
                }
                if (!GalleryAicrTraceFilter.shouldTraceMethod(method)) {
                    return;
                }
                String caller = param.args[1] instanceof String value ? value : "";
                ModernXposed.log(TAG + ": provider request " + providerName + " " + method
                        + " uid=" + Binder.getCallingUid() + " arg=" + caller + " "
                        + summarize(extras));
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String method = param.args[0] instanceof String value ? value : null;
                if (!GalleryAicrTraceFilter.shouldTraceMethod(method)) {
                    return;
                }
                Bundle result = param.getResult() instanceof Bundle bundle ? bundle : null;
                if (role == AicrProviderHookSpec.Role.NLS
                        && "get_progress".equals(method)
                        && result != null
                        && configClient.shouldBypass(Policy.AI_UI_CAPABILITY)) {
                    int progress = result.getInt("progress", 0);
                    AicrUiProgressCompatibility.Result normalized =
                            AicrUiProgressCompatibility.normalize(
                                    true,
                                    progress,
                                    result.containsKey("is_support_ai_search_progress"),
                                    result.getInt("is_support_ai_search_progress", -1),
                                    result.containsKey("in_progress"),
                                    result.getInt("in_progress", -1)
                            );
                    result.putInt("is_support_ai_search_progress", normalized.support());
                    result.putInt("in_progress", normalized.inProgress());
                    if (normalized.changed()) {
                        ModernXposed.log(TAG + ": normalized Gallery AI UI capability progress="
                                + progress + " state=" + normalized.inProgress());
                    }
                }
                ModernXposed.log(TAG + ": provider response " + providerName + " " + method
                        + " " + summarize(result));
            }
        };
    }

    static Action decide(
            AicrProviderHookSpec.Role role,
            String method,
            boolean isRunAlgo,
            boolean shouldPause
    ) {
        if (!shouldPause) {
            return Action.ALLOW;
        }
        if (role == AicrProviderHookSpec.Role.DATABASE
                && ("method_algo_analyse_start".equals(method)
                || "method_algo_analyse_UNLIMITED".equals(method))) {
            return Action.BLOCK_DATABASE_START;
        }
        if (role == AicrProviderHookSpec.Role.UI
                && "method_change_algo_state".equals(method)
                && isRunAlgo) {
            return Action.CONVERT_UI_START_TO_PAUSE;
        }
        return Action.ALLOW;
    }

    enum Action {
        ALLOW,
        BLOCK_DATABASE_START,
        CONVERT_UI_START_TO_PAUSE
    }

    private static String summarize(Bundle bundle) {
        if (bundle == null) {
            return "{}";
        }
        StringBuilder summary = new StringBuilder("{");
        boolean first = true;
        for (String field : FIELDS) {
            if (!GalleryAicrTraceFilter.shouldLogField(field) || !bundle.containsKey(field)) {
                continue;
            }
            if (!first) {
                summary.append(", ");
            }
            summary.append(field).append('=').append(bundle.get(field));
            first = false;
        }
        return summary.append('}').toString();
    }

    private static MethodData selectUnambiguous(List<MethodData> candidates) {
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        List<MethodData> named = candidates.stream()
                .filter(candidate -> candidate.getMethodName().equals("call"))
                .collect(Collectors.toList());
        return named.size() == 1 ? named.get(0) : null;
    }

    private static List<String> parameterNames(Method method) {
        List<String> names = new ArrayList<>();
        for (Class<?> parameter : method.getParameterTypes()) {
            names.add(parameter.getName());
        }
        return names;
    }

    private static String descriptor(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName()
                + "(" + String.join(",", parameterNames(method)) + ")";
    }

    private static String simpleName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    public record InstallResult(
            int installedCount,
            boolean uiCompatibilityInstalled,
            boolean databaseStartGateInstalled,
            boolean uiStartGateInstalled
    ) {
    }
}

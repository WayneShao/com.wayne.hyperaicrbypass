package com.wayne.hyperaicrbypass.hook;

import android.os.Binder;
import android.os.Bundle;

import com.wayne.hyperaicrbypass.config.ConfigClient;
import com.wayne.hyperaicrbypass.config.Policy;

import java.util.List;

import com.wayne.hyperaicrbypass.xposed.ModernHook;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;
import com.wayne.hyperaicrbypass.xposed.ReflectionHelpers;

public final class AicrProviderTraceHooks {
    private static final String TAG = "HyperAICRBypass";
    private static final List<String> PROVIDERS = List.of(
            "com.xiaomi.aicr.aisearch.provider.SearchDataBaseProvider",
            "com.xiaomi.aicr.aisearch.AISearchUIProvider",
            "com.xiaomi.aicr.aisearch.provider.NLSCapabilityProvider"
    );
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

    private final ClassLoader classLoader;
    private final ConfigClient configClient;

    public AicrProviderTraceHooks(ClassLoader classLoader, ConfigClient configClient) {
        this.classLoader = classLoader;
        this.configClient = configClient;
    }

    public InstallResult install() {
        int installed = 0;
        boolean uiCompatibilityInstalled = false;
        for (String provider : PROVIDERS) {
            if (install(provider)) {
                installed++;
                if (provider.endsWith(".NLSCapabilityProvider")) {
                    uiCompatibilityInstalled = true;
                }
            }
        }
        ModernXposed.log(TAG + ": AICR provider trace hooks=" + installed + "/"
                + PROVIDERS.size());
        return new InstallResult(installed, uiCompatibilityInstalled);
    }

    private boolean install(String providerClass) {
        try {
            ReflectionHelpers.findAndHookMethod(
                    providerClass,
                    classLoader,
                    "call",
                    String.class,
                    String.class,
                    Bundle.class,
                    callback(providerClass.substring(providerClass.lastIndexOf('.') + 1))
            );
            return true;
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": provider trace unavailable " + providerClass
                    + " -> " + error);
            return false;
        }
    }

    private ModernHook callback(String providerName) {
        return new ModernHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String method = param.args[0] instanceof String value ? value : null;
                if (!GalleryAicrTraceFilter.shouldTraceMethod(method)) {
                    return;
                }
                String caller = param.args[1] instanceof String value ? value : "";
                Bundle extras = param.args[2] instanceof Bundle bundle ? bundle : null;
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
                if ("NLSCapabilityProvider".equals(providerName)
                        && "get_progress".equals(method)
                        && result != null
                        && configClient.snapshot().shouldBypass(Policy.AI_UI_CAPABILITY)) {
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

    public record InstallResult(int installedCount, boolean uiCompatibilityInstalled) {
    }
}

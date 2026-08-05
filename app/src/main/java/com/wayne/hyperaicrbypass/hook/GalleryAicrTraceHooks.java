package com.wayne.hyperaicrbypass.hook;

import android.content.ContentResolver;
import android.content.ContentProviderClient;
import android.net.Uri;
import android.os.Bundle;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class GalleryAicrTraceHooks {
    private static final String TAG = "HyperAICRBypass";
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
            "is_run_algo"
    );

    private GalleryAicrTraceHooks() {
    }

    public static void install() {
        int installed = 0;
        installed += installResolverUriCall() ? 1 : 0;
        installed += installResolverAuthorityCall() ? 1 : 0;
        installed += installProviderClientCall() ? 1 : 0;
        XposedBridge.log(TAG + ": gallery AICR trace hooks=" + installed + "/3");
    }

    private static boolean installResolverUriCall() {
        try {
            XposedHelpers.findAndHookMethod(
                    ContentResolver.class,
                    "call",
                    Uri.class,
                    String.class,
                    String.class,
                    Bundle.class,
                    resolverUriCallback()
            );
            return true;
        } catch (Throwable error) {
            XposedBridge.log(TAG + ": gallery resolver Uri trace unavailable -> " + error);
            return false;
        }
    }

    private static boolean installResolverAuthorityCall() {
        try {
            XposedHelpers.findAndHookMethod(
                    ContentResolver.class,
                    "call",
                    String.class,
                    String.class,
                    String.class,
                    Bundle.class,
                    resolverAuthorityCallback()
            );
            return true;
        } catch (Throwable error) {
            XposedBridge.log(TAG + ": gallery resolver authority trace unavailable -> " + error);
            return false;
        }
    }

    private static boolean installProviderClientCall() {
        try {
            XposedHelpers.findAndHookMethod(
                    ContentProviderClient.class,
                    "call",
                    String.class,
                    String.class,
                    Bundle.class,
                    providerClientCallback()
            );
            return true;
        } catch (Throwable error) {
            XposedBridge.log(TAG + ": gallery provider-client trace unavailable -> " + error);
            return false;
        }
    }

    private static XC_MethodHook resolverUriCallback() {
        return callback(param -> {
            Uri uri = param.args[0] instanceof Uri value ? value : null;
            String authority = uri == null ? null : uri.getAuthority();
            String method = param.args[1] instanceof String value ? value : null;
            return GalleryAicrTraceFilter.shouldTrace(authority, method)
                    ? new TraceTarget(authority, method, 3) : null;
        });
    }

    private static XC_MethodHook resolverAuthorityCallback() {
        return callback(param -> {
            String authority = param.args[0] instanceof String value ? value : null;
            String method = param.args[1] instanceof String value ? value : null;
            return GalleryAicrTraceFilter.shouldTrace(authority, method)
                    ? new TraceTarget(authority, method, 3) : null;
        });
    }

    private static XC_MethodHook providerClientCallback() {
        return callback(param -> {
            String method = param.args[0] instanceof String value ? value : null;
            return GalleryAicrTraceFilter.shouldTraceMethod(method)
                    ? new TraceTarget("provider-client", method, 2) : null;
        });
    }

    private static XC_MethodHook callback(TargetResolver resolver) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                TraceTarget target = resolver.resolve(param);
                if (target == null) {
                    return;
                }
                Bundle extras = param.args[target.extrasIndex] instanceof Bundle bundle
                        ? bundle : null;
                XposedBridge.log(TAG + ": gallery AICR request "
                        + target.authority + " " + target.method + " " + summarize(extras));
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                TraceTarget target = resolver.resolve(param);
                if (target == null) {
                    return;
                }
                Bundle result = param.getResult() instanceof Bundle bundle ? bundle : null;
                XposedBridge.log(TAG + ": gallery AICR response "
                        + target.authority + " " + target.method + " " + summarize(result));
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

    private interface TargetResolver {
        TraceTarget resolve(XC_MethodHook.MethodHookParam param);
    }

    private record TraceTarget(String authority, String method, int extrasIndex) {
    }
}

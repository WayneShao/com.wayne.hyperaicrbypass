package com.wayne.hyperaicrbypass.hook;

import android.content.Context;
import android.content.Intent;

import com.wayne.hyperaicrbypass.adapt.DexKitBridgeFactory;
import com.wayne.hyperaicrbypass.config.BrowserConfig;
import com.wayne.hyperaicrbypass.config.BrowserConfigClient;
import com.wayne.hyperaicrbypass.xposed.ModernHook;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;

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

public final class CopyWebsiteBrowserHooks {
    private static final String TAG = "HyperAICRBypass";

    private final Context context;
    private final ClassLoader classLoader;
    private final AicrVersionBranch branch;
    private final BrowserConfigClient configClient;
    private final Set<String> installed = new HashSet<>();

    public CopyWebsiteBrowserHooks(Context context, AicrVersionBranch branch) {
        this.context = context;
        this.classLoader = context.getClassLoader();
        this.branch = branch;
        this.configClient = new BrowserConfigClient(context);
    }

    public synchronized InstallResult install() {
        int count = 0;
        int expected = CopyWebsiteBrowserHookCatalog.forBranch(branch).size();
        try (DexKitBridge bridge = DexKitBridgeFactory.create(context)) {
            for (CopyWebsiteBrowserHookCatalog.Spec spec
                    : CopyWebsiteBrowserHookCatalog.forBranch(branch)) {
                Method method = findUnique(bridge, spec);
                if (method != null && install(method, spec.kind())) {
                    count++;
                }
            }
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": browser discovery unavailable -> " + error);
        }
        ModernXposed.log(TAG + ": copy website browser hooks=" + count + "/" + expected
                + " branch=" + branch);
        return new InstallResult(count, expected);
    }

    private Method findUnique(DexKitBridge bridge, CopyWebsiteBrowserHookCatalog.Spec spec) {
        try {
            MethodMatcher matcher = MethodMatcher.create()
                    .returnType(spec.returnType())
                    .paramTypes(spec.parameterTypes())
                    .usingStrings(spec.anchor());
            List<MethodData> candidates = new ArrayList<>(bridge.findMethod(
                    FindMethod.create().matcher(matcher)
            ));
            List<Method> valid = new ArrayList<>();
            for (MethodData data : candidates) {
                if (!CopyWebsiteBrowserHookCatalog.isExpectedOwner(branch, data.getClassName())
                        || !Modifier.isStatic(data.getModifiers())) {
                    continue;
                }
                Method method = data.getMethodInstance(classLoader);
                if (matches(method, spec)) {
                    valid.add(method);
                }
            }
            if (valid.size() != 1) {
                ModernXposed.log(TAG + ": browser hook ambiguous kind=" + spec.kind()
                        + " candidates=" + candidates.size() + " valid=" + valid.size());
                return null;
            }
            return valid.get(0);
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": browser query failed kind=" + spec.kind()
                    + " -> " + error);
            return null;
        }
    }

    private boolean install(Method method, CopyWebsiteBrowserHookCatalog.Kind kind) {
        String descriptor = method.toGenericString();
        if (installed.contains(descriptor)) {
            return true;
        }
        try {
            ModernXposed.hookMethod(method, callback(kind));
            installed.add(descriptor);
            ModernXposed.log(TAG + ": browser hook -> " + descriptor);
            return true;
        } catch (Throwable error) {
            ModernXposed.log(TAG + ": browser registration failed -> " + error);
            return false;
        }
    }

    private ModernHook callback(CopyWebsiteBrowserHookCatalog.Kind kind) {
        return new ModernHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                BrowserConfig config = configClient.snapshot();
                if (!config.enabled() || kind != CopyWebsiteBrowserHookCatalog.Kind.OPEN_URL) {
                    return;
                }
                if (param.args.length >= 2 && param.args[0] instanceof Context targetContext
                        && param.args[1] instanceof String url) {
                    CopyWebsiteBrowser.open(targetContext, url, config.packageName());
                    param.setResult(null);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                BrowserConfig config = configClient.snapshot();
                if (!config.enabled()
                        || kind != CopyWebsiteBrowserHookCatalog.Kind.RETURN_INTENT
                        || !(param.getResult() instanceof Intent intent)) {
                    return;
                }
                CopyWebsiteBrowser.applyPackage(context, intent, config.packageName());
            }
        };
    }

    private static boolean matches(Method method, CopyWebsiteBrowserHookCatalog.Spec spec) {
        if (!method.getReturnType().getName().equals(spec.returnType())
                || method.getParameterCount() != spec.parameterTypes().size()
                || !Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        for (int i = 0; i < method.getParameterCount(); i++) {
            if (!method.getParameterTypes()[i].getName().equals(spec.parameterTypes().get(i))) {
                return false;
            }
        }
        return true;
    }

    public record InstallResult(int installedCount, int expectedCount) {
        public boolean complete() {
            return installedCount >= expectedCount;
        }
    }
}

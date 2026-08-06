package com.wayne.hyperaicrbypass.hook;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.wayne.hyperaicrbypass.config.ConfigClient;
import com.wayne.hyperaicrbypass.config.Policy;

import com.wayne.hyperaicrbypass.xposed.ModernHook;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;
import com.wayne.hyperaicrbypass.xposed.ReflectionHelpers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.EnumMap;
import java.util.Map;

public final class HookBootstrap {
    private static final String TAG = "HyperAICRBypass";
    private static boolean initialized;

    private HookBootstrap() {
    }

    public static void installAfterAttach(String packageName, String processName) {
        ReflectionHelpers.findAndHookMethod(
                Application.class,
                "attach",
                Context.class,
                new ModernHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Context context = (Context) param.args[0];
                        initializeOnce(packageName, processName, context);
                    }
                }
        );
    }

    private static synchronized void initializeOnce(
            String packageName,
            String processName,
            Context context
    ) {
        if (initialized) {
            return;
        }
        initialized = true;
        ConfigClient client = new ConfigClient(context);
        ModernXposed.log(TAG + ": bootstrap " + packageName + " process=" + processName
                + " revision=" + client.snapshot().getConfigRevision());
        if ("com.miui.gallery".equals(packageName)) {
            GalleryAicrTraceHooks.install();
            return;
        }
        if ("com.xiaomi.aicr".equals(packageName)) {
            PowerSaveExecutionHooks powerSaveHooks =
                    new PowerSaveExecutionHooks(context, client);
            PowerSaveExecutionHooks.InstallResult powerSaveResult = powerSaveHooks.install();
            int preciseProgressCount = new PreciseProgressHooks(context, client).install();
            int globalPreciseProgressCount =
                    new GlobalPreciseProgressHooks(context, client).install();
            AicrProviderTraceHooks.InstallResult providerHooks =
                    new AicrProviderTraceHooks(context.getClassLoader(), client).install();
            int compatibilityCount = new RunningStatusCompatibilityHooks(
                    context.getClassLoader(), client
            ).install();
            ExactAicrHooks.InstallResult exact =
                    new ExactAicrHooks(context.getClassLoader(), client).install();
            SemanticHooks semantic = new SemanticHooks(
                    context, client, exact.registeredIds()
            );
            int semanticCount = semantic.install(exact.missingSpecs());
            PolicyCoverageReporter coverageReporter = new PolicyCoverageReporter(context);
            coverageReporter.report(
                    exact.policyCounts(),
                    coverageWithProviderFallback(
                            semantic.policyCounts(), providerHooks.uiCompatibilityInstalled()
                    ),
                    client.snapshot().getRescanGeneration()
            );
            RescanGenerationGate rescanGate = new RescanGenerationGate(
                    client.snapshot().getRescanGeneration()
            );
            ExecutorService rescanExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "HyperAICRBypass-rescan");
                thread.setDaemon(true);
                return thread;
            });
            client.setListener(config -> {
                if (rescanGate.tryAdvance(config.getRescanGeneration())) {
                    rescanExecutor.execute(() -> {
                        semantic.install(exact.missingSpecs());
                        coverageReporter.report(
                                exact.policyCounts(),
                                coverageWithProviderFallback(
                                        semantic.policyCounts(),
                                        providerHooks.uiCompatibilityInstalled()
                                ),
                                config.getRescanGeneration()
                        );
                    });
                }
            });
            if (exact.aicrHookCount() + semanticCount + compatibilityCount
                    + providerHooks.installedCount() + preciseProgressCount
                    + globalPreciseProgressCount + powerSaveResult.installedCount() == 0) {
                showTotalFailureOnce(context);
            }
        }
    }

    static Map<Policy, Integer> coverageWithProviderFallback(
            Map<Policy, Integer> semanticCounts,
            boolean providerInstalled
    ) {
        EnumMap<Policy, Integer> result = new EnumMap<>(Policy.class);
        result.putAll(semanticCounts);
        if (providerInstalled) {
            result.merge(Policy.AI_UI_CAPABILITY, 1, Integer::sum);
        }
        return result;
    }

    private static void showTotalFailureOnce(Context context) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(
                context,
                "Hook 适配失败，请联系作者更新模块",
                Toast.LENGTH_LONG
        ).show());
    }
}

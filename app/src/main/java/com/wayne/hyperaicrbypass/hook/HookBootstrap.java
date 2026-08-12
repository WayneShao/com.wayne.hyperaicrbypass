package com.wayne.hyperaicrbypass.hook;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.wayne.hyperaicrbypass.config.ConfigClient;
import com.wayne.hyperaicrbypass.config.Policy;
import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;

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
            long versionCode = AicrPackageVersion.read(context);
            AicrVersionBranch branch = AicrPackageVersion.branch(context);
            ModernXposed.log(TAG + ": AICR versionCode=" + versionCode
                    + " branch=" + branch);
            CopyWebsiteBrowserHooks browserHooks =
                    new CopyWebsiteBrowserHooks(context, branch);
            CopyWebsiteBrowserHooks.InstallResult browserResult = browserHooks.install();
            ExecutionCoverageReporter executionCoverageReporter =
                    new ExecutionCoverageReporter(context);
            DiscoveryKey executionKey = executionCoverageReporter.reportPending(
                    client.snapshot().getRescanGeneration()
            );
            BrowserHookCoverageReporter browserCoverageReporter =
                    new BrowserHookCoverageReporter(context);
            browserCoverageReporter.report(executionKey, browserResult);
            PowerSaveExecutionHooks powerSaveHooks =
                    new PowerSaveExecutionHooks(context, client);
            PowerSaveExecutionHooks.InstallResult powerSaveResult = powerSaveHooks.install();
            PreciseProgressHooks preciseProgressHooks =
                    new PreciseProgressHooks(context, client);
            PreciseProgressCoverageReporter preciseCoverageReporter =
                    new PreciseProgressCoverageReporter(context);
            int preciseProgressCount = preciseProgressHooks.install();
            GlobalPreciseProgressHooks globalPreciseProgressHooks =
                    new GlobalPreciseProgressHooks(context, client);
            int globalPreciseProgressCount = globalPreciseProgressHooks.install();
            preciseCoverageReporter.report(
                    executionKey,
                    preciseProgressCount + globalPreciseProgressCount,
                    PreciseProgressHookCatalog.points().size()
                            + GlobalProgressHookCatalog.points(branch).size()
            );
            AicrProviderTraceHooks providerHookInstaller =
                    new AicrProviderTraceHooks(context, client);
            AicrProviderTraceHooks.InstallResult providerHooks =
                    providerHookInstaller.install();
            executionCoverageReporter.report(executionKey, powerSaveResult, providerHooks);
            int compatibilityCount = new RunningStatusCompatibilityHooks(
                    context.getClassLoader(), client
            ).install();
            ExactAicrHooks.InstallResult exact =
                    new ExactAicrHooks(
                            context.getClassLoader(), client, branch
                    ).install();
            SemanticHooks semantic = new SemanticHooks(
                    context, client, exact.registeredIds(), branch
            );
            int semanticCount = semantic.install(exact.missingSpecs());
            PolicyCoverageReporter coverageReporter =
                    new PolicyCoverageReporter(context, branch);
            coverageReporter.report(
                    exact.policyCounts(),
                    coverageWithProviderFallback(
                            semantic.policyCounts(), providerHooks.uiCompatibilityInstalled()
                    ),
                    executionKey
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
                        DiscoveryKey rescanKey = executionCoverageReporter.reportPending(
                                config.getRescanGeneration()
                        );
                        PowerSaveExecutionHooks.InstallResult rescannedPowerSave =
                                powerSaveHooks.install();
                        AicrProviderTraceHooks.InstallResult rescannedProviders =
                                providerHookInstaller.install();
                        int rescannedPreciseProgress = preciseProgressHooks.install();
                        int rescannedGlobalProgress = globalPreciseProgressHooks.install();
                        CopyWebsiteBrowserHooks.InstallResult rescannedBrowser =
                                browserHooks.install();
                        browserCoverageReporter.report(rescanKey, rescannedBrowser);
                        preciseCoverageReporter.report(
                                rescanKey,
                                rescannedPreciseProgress + rescannedGlobalProgress,
                                PreciseProgressHookCatalog.points().size()
                                        + GlobalProgressHookCatalog.points(branch).size()
                        );
                        semantic.install(exact.missingSpecs());
                        coverageReporter.report(
                                exact.policyCounts(),
                                coverageWithProviderFallback(
                                        semantic.policyCounts(),
                                        rescannedProviders.uiCompatibilityInstalled()
                                ),
                                rescanKey
                        );
                        executionCoverageReporter.report(
                                rescanKey, rescannedPowerSave, rescannedProviders
                        );
                    });
                }
            });
            if (exact.aicrHookCount() + semanticCount + compatibilityCount
                    + providerHooks.installedCount() + preciseProgressCount
                    + globalPreciseProgressCount + powerSaveResult.installedCount()
                    + browserResult.installedCount() == 0) {
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

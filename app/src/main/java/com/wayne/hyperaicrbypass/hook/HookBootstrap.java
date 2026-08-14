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
import java.util.Set;

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
            AicrRuntimeLayout layout = AicrRuntimeLayout.detect(
                    branch, context.getClassLoader());
            AicrProcessRole processRole = AicrProcessRole.forProcess(processName);
            ModernXposed.log(TAG + ": AICR versionCode=" + versionCode
                    + " branch=" + branch + " layout=" + layout
                    + " role=" + processRole);
            if (processRole == AicrProcessRole.IGNORE) {
                ModernXposed.log(TAG + ": skip non-functional AICR process=" + processName);
                return;
            }
            if (processRole == AicrProcessRole.MAIN) {
                CopyWebsiteBrowserHooks browserHooks =
                        new CopyWebsiteBrowserHooks(context, layout);
                CopyWebsiteBrowserHooks.InstallResult browserResult = browserHooks.install();
                DiscoveryKey key = new ExecutionCoverageReporter(context).currentKey(
                        client.snapshot().getRescanGeneration());
                new BrowserHookCoverageReporter(context).report(key, browserResult);
                return;
            }
            if (processRole == AicrProcessRole.SEARCH_SERVICE) {
                new AicrProviderTraceHooks(context, client).install(
                        Set.of(AicrProviderHookSpec.Role.NLS));
                return;
            }
            if (processRole == AicrProcessRole.SEARCH_UI) {
                installSearchUiHooks(context, client, layout);
                return;
            }
            ExecutionCoverageReporter executionCoverageReporter =
                    new ExecutionCoverageReporter(context);
            DiscoveryKey executionKey = executionCoverageReporter.reportPending(
                    client.snapshot().getRescanGeneration()
            );
            PowerSaveExecutionHooks powerSaveHooks =
                    new PowerSaveExecutionHooks(context, client, layout);
            PowerSaveExecutionHooks.InstallResult powerSaveResult = powerSaveHooks.install();
            PreciseProgressHooks preciseProgressHooks =
                    new PreciseProgressHooks(context, client, layout);
            PreciseProgressCoverageReporter preciseCoverageReporter =
                    new PreciseProgressCoverageReporter(context);
            int preciseProgressCount = preciseProgressHooks.install();
            GlobalPreciseProgressHooks globalPreciseProgressHooks =
                    new GlobalPreciseProgressHooks(context, client, layout);
            int globalPreciseProgressCount = globalPreciseProgressHooks.install();
            preciseCoverageReporter.report(
                    executionKey,
                    preciseProgressCount + globalPreciseProgressCount,
                    PreciseProgressHookCatalog.points(layout).size()
                            + GlobalProgressHookCatalog.points(layout).size()
            );
            AicrProviderTraceHooks providerHookInstaller =
                    new AicrProviderTraceHooks(context, client);
            AicrProviderTraceHooks.InstallResult providerHooks =
                    providerHookInstaller.install(Set.of(
                            AicrProviderHookSpec.Role.DATABASE,
                            AicrProviderHookSpec.Role.UI));
            executionCoverageReporter.report(executionKey, powerSaveResult, providerHooks);
            int compatibilityCount = new RunningStatusCompatibilityHooks(
                            context.getClassLoader(), client
            ).install();
            ExactAicrHooks.InstallResult exact =
                    new ExactAicrHooks(
                            context.getClassLoader(), client, layout
                    ).install();
            SemanticHooks semantic = new SemanticHooks(
                    context, client, exact.registeredIds(), layout
            );
            int semanticCount = semantic.install(exact.missingSpecs());
            PolicyCoverageReporter coverageReporter =
                    new PolicyCoverageReporter(context, layout);
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
                                providerHookInstaller.install(Set.of(
                                        AicrProviderHookSpec.Role.DATABASE,
                                        AicrProviderHookSpec.Role.UI));
                        int rescannedPreciseProgress = preciseProgressHooks.install();
                        int rescannedGlobalProgress = globalPreciseProgressHooks.install();
                        preciseCoverageReporter.report(
                                rescanKey,
                                rescannedPreciseProgress + rescannedGlobalProgress,
                                PreciseProgressHookCatalog.points(layout).size()
                                        + GlobalProgressHookCatalog.points(layout).size()
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
            int coreHookCount = exact.aicrHookCount() + semanticCount + compatibilityCount
                    + providerHooks.installedCount() + preciseProgressCount
                    + globalPreciseProgressCount + powerSaveResult.installedCount();
            if (shouldShowTotalFailure(processRole, coreHookCount)) {
                showTotalFailureOnce(context);
            }
        }
    }

    private static void installSearchUiHooks(
            Context context,
            ConfigClient client,
            AicrRuntimeLayout layout
    ) {
        AicrProviderTraceHooks providerHooks = new AicrProviderTraceHooks(context, client);
        providerHooks.install(Set.of(AicrProviderHookSpec.Role.UI));
        PreciseProgressHooks precise = new PreciseProgressHooks(context, client, layout);
        GlobalPreciseProgressHooks global =
                new GlobalPreciseProgressHooks(context, client, layout);
        PreciseProgressCoverageReporter reporter =
                new PreciseProgressCoverageReporter(context);
        ExecutionCoverageReporter keyReporter = new ExecutionCoverageReporter(context);
        int preciseCount = precise.install();
        int globalCount = global.install();
        reporter.report(
                keyReporter.currentKey(client.snapshot().getRescanGeneration()),
                preciseCount + globalCount,
                PreciseProgressHookCatalog.points(layout).size()
                        + GlobalProgressHookCatalog.points(layout).size()
        );
        RescanGenerationGate gate = new RescanGenerationGate(
                client.snapshot().getRescanGeneration());
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "HyperAICRBypass-ui-rescan");
            thread.setDaemon(true);
            return thread;
        });
        client.setListener(config -> {
            if (!gate.tryAdvance(config.getRescanGeneration())) {
                return;
            }
            executor.execute(() -> {
                providerHooks.install(Set.of(AicrProviderHookSpec.Role.UI));
                int rescannedPrecise = precise.install();
                int rescannedGlobal = global.install();
                reporter.report(
                        keyReporter.currentKey(config.getRescanGeneration()),
                        rescannedPrecise + rescannedGlobal,
                        PreciseProgressHookCatalog.points(layout).size()
                                + GlobalProgressHookCatalog.points(layout).size()
                );
            });
        });
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

    static boolean shouldShowTotalFailure(AicrProcessRole role, int installedHookCount) {
        return role == AicrProcessRole.SEARCH_DATA && installedHookCount == 0;
    }

    private static void showTotalFailureOnce(Context context) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(
                context,
                "Hook 适配失败，请联系作者更新模块",
                Toast.LENGTH_LONG
        ).show());
    }
}

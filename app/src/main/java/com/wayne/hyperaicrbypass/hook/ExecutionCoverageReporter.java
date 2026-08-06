package com.wayne.hyperaicrbypass.hook;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;

import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;
import com.wayne.hyperaicrbypass.config.BypassSettingsProvider;
import com.wayne.hyperaicrbypass.config.ConfigContract;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;

public final class ExecutionCoverageReporter {
    public static final int SCHEMA_REVISION = 3;
    private static final String TAG = "HyperAICRBypass";

    private final Context context;

    public ExecutionCoverageReporter(Context context) {
        this.context = context;
    }

    public DiscoveryKey reportPending(long rescanGeneration) {
        DiscoveryKey key = discoveryKey(rescanGeneration);
        report(key, ExecutionCoverage.PENDING);
        return key;
    }

    public ExecutionCoverage report(
            DiscoveryKey key,
            PowerSaveExecutionHooks.InstallResult execution,
            AicrProviderTraceHooks.InstallResult providers
    ) {
        ExecutionCoverage coverage = ExecutionCoverage.assess(
                execution.startInstalled(),
                execution.needStopInstalled(),
                providers.databaseStartGateInstalled(),
                providers.uiStartGateInstalled()
        );
        report(key, coverage);
        return coverage;
    }

    private DiscoveryKey discoveryKey(long rescanGeneration) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo("com.xiaomi.aicr", 0);
            return new DiscoveryKey(
                    info.getLongVersionCode(),
                    info.lastUpdateTime,
                    SCHEMA_REVISION,
                    rescanGeneration
            );
        } catch (Exception ignored) {
            return new DiscoveryKey(0L, 0L, SCHEMA_REVISION, rescanGeneration);
        }
    }

    private void report(DiscoveryKey key, ExecutionCoverage coverage) {
        Bundle extras = new Bundle();
        extras.putString(ConfigContract.KEY_EXECUTION_COVERAGE, coverage.name());
        extras.putLong(ConfigContract.KEY_DISCOVERY_VERSION_CODE, key.versionCode());
        extras.putLong(ConfigContract.KEY_DISCOVERY_UPDATE_TIME, key.lastUpdateTime());
        extras.putInt(ConfigContract.KEY_DISCOVERY_SCHEMA_REVISION, key.schemaRevision());
        extras.putLong(ConfigContract.KEY_RESCAN_GENERATION, key.rescanGeneration());
        try {
            context.getContentResolver().call(
                    BypassSettingsProvider.CONTENT_URI,
                    ConfigContract.METHOD_REPORT_EXECUTION_COVERAGE,
                    null,
                    extras
            );
        } catch (RuntimeException error) {
            ModernXposed.log(TAG + ": execution coverage report failed -> " + error);
        }
    }
}

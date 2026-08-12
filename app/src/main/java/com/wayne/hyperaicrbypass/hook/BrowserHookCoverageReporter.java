package com.wayne.hyperaicrbypass.hook;

import android.content.Context;
import android.os.Bundle;

import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;
import com.wayne.hyperaicrbypass.config.BypassSettingsProvider;
import com.wayne.hyperaicrbypass.config.ConfigContract;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;

final class BrowserHookCoverageReporter {
    private static final String TAG = "HyperAICRBypass";
    private final Context context;

    BrowserHookCoverageReporter(Context context) {
        this.context = context;
    }

    void report(DiscoveryKey key, CopyWebsiteBrowserHooks.InstallResult result) {
        Bundle extras = new Bundle();
        extras.putString(ConfigContract.KEY_BROWSER_DISCOVERY_KEY, key.stableValue());
        extras.putString(ConfigContract.KEY_BROWSER_COVERAGE,
                BrowserHookCoverage.assess(
                        result.installedCount(), result.expectedCount()
                ).name());
        extras.putInt(ConfigContract.KEY_BROWSER_HOOK_COUNT, result.installedCount());
        extras.putInt(ConfigContract.KEY_BROWSER_HOOK_EXPECTED, result.expectedCount());
        try {
            context.getContentResolver().call(
                    BypassSettingsProvider.CONTENT_URI,
                    ConfigContract.METHOD_REPORT_BROWSER_COVERAGE,
                    null,
                    extras
            );
        } catch (RuntimeException error) {
            ModernXposed.log(TAG + ": browser coverage report failed -> " + error);
        }
    }
}

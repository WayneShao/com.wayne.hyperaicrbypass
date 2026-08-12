package com.wayne.hyperaicrbypass.hook;

import android.content.Context;
import android.os.Bundle;

import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;
import com.wayne.hyperaicrbypass.config.BypassSettingsProvider;
import com.wayne.hyperaicrbypass.config.ConfigContract;
import com.wayne.hyperaicrbypass.xposed.ModernXposed;

final class PreciseProgressCoverageReporter {
    private static final String TAG = "HyperAICRBypass";
    private final Context context;

    PreciseProgressCoverageReporter(Context context) {
        this.context = context;
    }

    void report(DiscoveryKey key, int installed, int expected) {
        Bundle extras = new Bundle();
        extras.putString(ConfigContract.KEY_PRECISE_PROGRESS_COVERAGE,
                PreciseProgressCoverage.assess(installed, expected).name());
        extras.putInt(ConfigContract.KEY_PRECISE_PROGRESS_COUNT, installed);
        extras.putInt(ConfigContract.KEY_PRECISE_PROGRESS_EXPECTED, expected);
        extras.putString(ConfigContract.KEY_PRECISE_PROGRESS_DISCOVERY_KEY,
                key.stableValue());
        try {
            context.getContentResolver().call(
                    BypassSettingsProvider.CONTENT_URI,
                    ConfigContract.METHOD_REPORT_PRECISE_PROGRESS_COVERAGE,
                    null,
                    extras
            );
        } catch (RuntimeException error) {
            ModernXposed.log(TAG + ": precise coverage report failed -> " + error);
        }
    }
}

package com.wayne.hyperaicrbypass.config;

import android.content.Context;
import android.content.SharedPreferences;

final class BrowserConfigStore {
    private static final String PREFERENCES = "browser_settings";

    private final SharedPreferences preferences;

    BrowserConfigStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    synchronized BrowserConfig read() {
        return new BrowserConfig(
                preferences.getBoolean(ConfigContract.KEY_BROWSER_ENABLED, false),
                preferences.getString(ConfigContract.KEY_BROWSER_PACKAGE,
                        BrowserConfig.SYSTEM_DEFAULT)
        );
    }

    synchronized BrowserConfig update(boolean enabled, String packageName) {
        BrowserConfig updated = new BrowserConfig(enabled, packageName);
        if (!preferences.edit()
                .putBoolean(ConfigContract.KEY_BROWSER_ENABLED, updated.enabled())
                .putString(ConfigContract.KEY_BROWSER_PACKAGE, updated.packageName())
                .commit()) {
            throw new IllegalStateException("Unable to persist browser settings");
        }
        return updated;
    }
}

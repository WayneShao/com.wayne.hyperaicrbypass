package com.wayne.hyperaicrbypass.config;

import java.util.Objects;

public record BrowserConfig(boolean enabled, String packageName) {
    public static final String SYSTEM_DEFAULT = "";

    public BrowserConfig {
        Objects.requireNonNull(packageName);
        ConfigContract.requireShortText(packageName, ConfigContract.KEY_BROWSER_PACKAGE);
    }

    public static BrowserConfig defaults() {
        return new BrowserConfig(false, SYSTEM_DEFAULT);
    }
}

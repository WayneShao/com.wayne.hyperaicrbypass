package com.wayne.hyperaicrbypass.config;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicReference;

public final class BrowserConfigClient implements AutoCloseable {
    private final ContentResolver resolver;
    private final AtomicReference<BrowserConfig> snapshot =
            new AtomicReference<>(BrowserConfig.defaults());
    private final ContentObserver observer;
    private boolean observing;

    public BrowserConfigClient(Context context) {
        Context application = context.getApplicationContext();
        resolver = (application == null ? context : application).getContentResolver();
        observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                refresh();
            }
        };
        try {
            resolver.registerContentObserver(BypassSettingsProvider.CONTENT_URI, false, observer);
            observing = true;
        } catch (RuntimeException ignored) {
            observing = false;
        }
        refresh();
    }

    public BrowserConfig snapshot() {
        return snapshot.get();
    }

    public void refresh() {
        try {
            Bundle response = resolver.call(
                    BypassSettingsProvider.CONTENT_URI,
                    ConfigContract.METHOD_GET_BROWSER_CONFIG,
                    null,
                    null
            );
            if (response != null) {
                snapshot.set(decode(response));
            }
        } catch (RuntimeException ignored) {
            // Keep the last complete snapshot while the provider is unavailable.
        }
    }

    public static Bundle encode(BrowserConfig config) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ConfigContract.KEY_BROWSER_ENABLED, config.enabled());
        bundle.putString(ConfigContract.KEY_BROWSER_PACKAGE, config.packageName());
        return bundle;
    }

    public static BrowserConfig decode(Bundle bundle) {
        if (!bundle.containsKey(ConfigContract.KEY_BROWSER_ENABLED)
                || !(bundle.get(ConfigContract.KEY_BROWSER_ENABLED) instanceof Boolean)
                || !(bundle.get(ConfigContract.KEY_BROWSER_PACKAGE) instanceof String value)) {
            throw new IllegalArgumentException("Malformed browser config");
        }
        return new BrowserConfig(
                bundle.getBoolean(ConfigContract.KEY_BROWSER_ENABLED), value
        );
    }

    @Override
    public void close() {
        if (observing) {
            try {
                resolver.unregisterContentObserver(observer);
            } catch (RuntimeException ignored) {
            }
            observing = false;
        }
    }
}

package com.wayne.hyperaicrbypass.config;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ConfigClient implements AutoCloseable {
    private final ContentResolver resolver;
    private final AtomicReference<BypassConfig> snapshot =
            new AtomicReference<>(BypassConfig.defaults());
    private final ContentObserver observer;
    private boolean observing;
    private volatile Consumer<BypassConfig> listener = config -> { };

    public ConfigClient(Context context) {
        Context applicationContext = context.getApplicationContext();
        Context resolverContext = applicationContext == null ? context : applicationContext;
        resolver = resolverContext.getContentResolver();
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

    public BypassConfig snapshot() {
        return snapshot.get();
    }

    public void setListener(Consumer<BypassConfig> listener) {
        this.listener = listener == null ? config -> { } : listener;
    }

    public void refresh() {
        try {
            Bundle response = resolver.call(
                    BypassSettingsProvider.CONTENT_URI,
                    ConfigContract.METHOD_GET_SNAPSHOT,
                    null,
                    null
            );
            acceptSnapshot(response);
        } catch (RuntimeException ignored) {
            // Keep the last known snapshot when the module provider is absent or malformed.
        }
    }

    public void acceptSnapshotForTest(Bundle response) {
        acceptSnapshot(response);
    }

    private void acceptSnapshot(Bundle response) {
        if (response == null) {
            return;
        }
        try {
            BypassConfig decoded = BundleConfigCodec.decode(response);
            BypassConfig previous = snapshot.getAndSet(decoded);
            if (!decoded.equals(previous)) {
                listener.accept(decoded);
            }
        } catch (RuntimeException ignored) {
            // Atomic replacement happens only after a complete snapshot validates.
        }
    }

    @Override
    public void close() {
        if (observing) {
            resolver.unregisterContentObserver(observer);
            observing = false;
        }
    }
}

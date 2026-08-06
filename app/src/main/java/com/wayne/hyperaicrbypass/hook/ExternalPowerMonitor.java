package com.wayne.hyperaicrbypass.hook;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class ExternalPowerMonitor implements AutoCloseable {
    private final Context context;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final BroadcastReceiver receiver;
    private volatile Consumer<Boolean> listener = ignored -> { };
    private boolean registered;

    public ExternalPowerMonitor(Context context) {
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext == null ? context : applicationContext;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ignored, Intent intent) {
                String action = intent == null ? null : intent.getAction();
                if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                    update(true);
                } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                    update(false);
                } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                    update(fromBatteryIntent(intent));
                }
            }
        };

        try {
            Intent sticky = this.context.registerReceiver(
                    null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            );
            if (sticky != null) {
                connected.set(fromBatteryIntent(sticky));
            }
        } catch (RuntimeException ignored) {
            connected.set(false);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                this.context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                this.context.registerReceiver(receiver, filter);
            }
            registered = true;
        } catch (RuntimeException ignored) {
            registered = false;
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void setListener(Consumer<Boolean> listener) {
        this.listener = listener == null ? ignored -> { } : listener;
    }

    @Override
    public void close() {
        if (!registered) {
            return;
        }
        try {
            context.unregisterReceiver(receiver);
        } catch (RuntimeException ignored) {
            // Receiver may already have been unregistered during process teardown.
        } finally {
            registered = false;
        }
    }

    static boolean isConnectedPowerSource(int plugged) {
        int supported = BatteryManager.BATTERY_PLUGGED_AC
                | BatteryManager.BATTERY_PLUGGED_USB
                | BatteryManager.BATTERY_PLUGGED_WIRELESS;
        return plugged > 0 && (plugged & supported) != 0;
    }

    private static boolean fromBatteryIntent(Intent intent) {
        return isConnectedPowerSource(
                intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        );
    }

    private void update(boolean value) {
        boolean previous = connected.getAndSet(value);
        if (previous != value) {
            listener.accept(value);
        }
    }
}

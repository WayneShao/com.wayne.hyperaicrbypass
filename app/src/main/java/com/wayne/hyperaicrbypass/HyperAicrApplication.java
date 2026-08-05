package com.wayne.hyperaicrbypass;

import android.app.Application;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class HyperAicrApplication extends Application
        implements XposedServiceHelper.OnServiceListener {
    public interface ActivationListener {
        void onActivationChanged(boolean active);
    }

    private static final Set<XposedService> SERVICES = new CopyOnWriteArraySet<>();
    private static final Set<ActivationListener> LISTENERS = new CopyOnWriteArraySet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(this);
    }

    public static boolean isModuleActive() {
        return !SERVICES.isEmpty();
    }

    public static void addActivationListener(ActivationListener listener) {
        LISTENERS.add(listener);
    }

    public static void removeActivationListener(ActivationListener listener) {
        LISTENERS.remove(listener);
    }

    @Override
    public void onServiceBind(XposedService service) {
        SERVICES.add(service);
        notifyActivationChanged();
    }

    @Override
    public void onServiceDied(XposedService service) {
        SERVICES.remove(service);
        notifyActivationChanged();
    }

    private static void notifyActivationChanged() {
        boolean active = isModuleActive();
        for (ActivationListener listener : LISTENERS) {
            listener.onActivationChanged(active);
        }
    }
}

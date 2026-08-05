package com.wayne.hyperaicrbypass.hook;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProcessBootstrapGate {
    private final Set<String> targetPackages;
    private boolean acquired;

    public ProcessBootstrapGate(List<String> targetPackages) {
        this.targetPackages = new HashSet<>(targetPackages);
    }

    public synchronized boolean tryAcquire(String packageName, String processName) {
        if (acquired || !targetPackages.contains(packageName)) {
            return false;
        }
        if (!processName.equals(packageName) && !processName.startsWith(packageName + ":")) {
            return false;
        }
        acquired = true;
        return true;
    }
}

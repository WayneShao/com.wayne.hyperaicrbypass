package com.wayne.hyperaicrbypass.hook;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class RegistrationPlanner {
    private final Set<String> registered = new HashSet<>();
    private final Map<String, String> failures = new HashMap<>();

    public synchronized boolean shouldAttempt(HookSpec spec) {
        return !registered.contains(spec.id());
    }

    public synchronized void recordSuccess(HookSpec spec) {
        registered.add(spec.id());
        failures.remove(spec.id());
    }

    public synchronized void recordFailure(HookSpec spec, String reason) {
        if (!registered.contains(spec.id())) {
            failures.put(spec.id(), reason == null ? "unknown" : reason);
        }
    }

    public synchronized boolean isRegistered(HookSpec spec) {
        return registered.contains(spec.id());
    }

    public synchronized int registeredCount() {
        return registered.size();
    }
}

package com.wayne.hyperaicrbypass.hook;

import android.os.Bundle;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class GlobalProgressPayload {
    private static final String PREFIX =
            "com.wayne.hyperaicrbypass.global_precise_progress.";
    static final String KEY_VERSION = PREFIX + "version";
    static final String KEY_THOUSANDTHS = PREFIX + "thousandths_percent";
    static final String KEY_FIXED_PROGRESS = PREFIX + "fixed_progress";
    static final String KEY_BRANCH = PREFIX + "branch";
    static final String KEY_RUN_START = PREFIX + "run_start_time";
    static final String KEY_GENERATION = PREFIX + "request_generation";
    static final String KEY_CAPTURED_ELAPSED = PREFIX + "captured_elapsed";
    private static final long VERSION = 1L;

    private GlobalProgressPayload() {
    }

    public static Map<String, Object> encode(GlobalProgressSnapshot snapshot) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(KEY_VERSION, VERSION);
        values.put(KEY_THOUSANDTHS, (long) snapshot.thousandthsPercent());
        values.put(KEY_FIXED_PROGRESS, (long) snapshot.fixedProgress());
        values.put(KEY_BRANCH, snapshot.branch().name());
        values.put(KEY_RUN_START, snapshot.runStartTime());
        values.put(KEY_GENERATION, snapshot.requestGeneration());
        values.put(KEY_CAPTURED_ELAPSED, snapshot.capturedElapsedRealtime());
        return Map.copyOf(values);
    }

    public static Optional<GlobalProgressSnapshot> decode(Map<String, ?> payload) {
        if (payload == null
                || !Long.valueOf(VERSION).equals(payload.get(KEY_VERSION))
                || !(payload.get(KEY_THOUSANDTHS) instanceof Long thousandths)
                || !(payload.get(KEY_FIXED_PROGRESS) instanceof Long fixed)
                || !(payload.get(KEY_BRANCH) instanceof String branch)
                || !(payload.get(KEY_RUN_START) instanceof Long runStart)
                || !(payload.get(KEY_GENERATION) instanceof Long generation)
                || !(payload.get(KEY_CAPTURED_ELAPSED) instanceof Long captured)
                || thousandths < Integer.MIN_VALUE
                || thousandths > Integer.MAX_VALUE
                || fixed < Integer.MIN_VALUE
                || fixed > Integer.MAX_VALUE) {
            return Optional.empty();
        }
        return GlobalProgressSnapshot.restore(
                thousandths.intValue(),
                fixed.intValue(),
                branch,
                runStart,
                generation,
                captured
        );
    }

    public static void writeToBundle(Bundle bundle, GlobalProgressSnapshot snapshot) {
        bundle.putLong(KEY_VERSION, VERSION);
        bundle.putLong(KEY_THOUSANDTHS, snapshot.thousandthsPercent());
        bundle.putLong(KEY_FIXED_PROGRESS, snapshot.fixedProgress());
        bundle.putString(KEY_BRANCH, snapshot.branch().name());
        bundle.putLong(KEY_RUN_START, snapshot.runStartTime());
        bundle.putLong(KEY_GENERATION, snapshot.requestGeneration());
        bundle.putLong(KEY_CAPTURED_ELAPSED, snapshot.capturedElapsedRealtime());
    }

    public static Optional<GlobalProgressSnapshot> readFromBundle(Bundle bundle) {
        if (bundle == null) {
            return Optional.empty();
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            for (String key : keys()) {
                if (!bundle.containsKey(key)) {
                    return Optional.empty();
                }
                payload.put(key, bundle.get(key));
            }
            return decode(payload);
        } catch (Throwable error) {
            return Optional.empty();
        }
    }

    private static String[] keys() {
        return new String[]{
                KEY_VERSION,
                KEY_THOUSANDTHS,
                KEY_FIXED_PROGRESS,
                KEY_BRANCH,
                KEY_RUN_START,
                KEY_GENERATION,
                KEY_CAPTURED_ELAPSED
        };
    }
}

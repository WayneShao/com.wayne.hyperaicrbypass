package com.wayne.hyperaicrbypass.hook;

import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PreciseProgressPayload {
    private static final String PREFIX =
            "com.wayne.hyperaicrbypass.precise_progress.";
    static final String KEY_VERSION = PREFIX + "version";
    static final String KEY_NUMERATOR = PREFIX + "numerator";
    static final String KEY_DENOMINATOR = PREFIX + "denominator";
    static final String KEY_FIXED_PROGRESS = PREFIX + "fixed_progress";
    static final String KEY_CAPTURED_ELAPSED = PREFIX + "captured_elapsed";
    private static final long VERSION = 1L;

    private PreciseProgressPayload() {
    }

    public static Map<String, Long> encode(PreciseProgressSnapshot snapshot) {
        return Map.of(
                KEY_VERSION, VERSION,
                KEY_NUMERATOR, snapshot.numerator(),
                KEY_DENOMINATOR, snapshot.denominator(),
                KEY_FIXED_PROGRESS, (long) snapshot.fixedProgress(),
                KEY_CAPTURED_ELAPSED, snapshot.capturedElapsedRealtime()
        );
    }

    public static Optional<PreciseProgressSnapshot> decode(Map<String, ?> payload) {
        if (payload == null) {
            return Optional.empty();
        }
        Long version = longValue(payload.get(KEY_VERSION));
        Long numerator = longValue(payload.get(KEY_NUMERATOR));
        Long denominator = longValue(payload.get(KEY_DENOMINATOR));
        Long fixedProgress = longValue(payload.get(KEY_FIXED_PROGRESS));
        Long capturedElapsed = longValue(payload.get(KEY_CAPTURED_ELAPSED));
        if (version == null
                || version != VERSION
                || numerator == null
                || denominator == null
                || fixedProgress == null
                || capturedElapsed == null
                || fixedProgress < Integer.MIN_VALUE
                || fixedProgress > Integer.MAX_VALUE) {
            return Optional.empty();
        }
        return PreciseProgressSnapshot.restore(
                numerator,
                denominator,
                fixedProgress.intValue(),
                capturedElapsed
        );
    }

    public static void writeToBundle(Bundle bundle, PreciseProgressSnapshot snapshot) {
        for (Map.Entry<String, Long> entry : encode(snapshot).entrySet()) {
            bundle.putLong(entry.getKey(), entry.getValue());
        }
    }

    public static Optional<PreciseProgressSnapshot> readFromBundle(Bundle bundle) {
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

    private static Long longValue(Object value) {
        return value instanceof Long longValue ? longValue : null;
    }

    private static String[] keys() {
        return new String[]{
                KEY_VERSION,
                KEY_NUMERATOR,
                KEY_DENOMINATOR,
                KEY_FIXED_PROGRESS,
                KEY_CAPTURED_ELAPSED
        };
    }
}

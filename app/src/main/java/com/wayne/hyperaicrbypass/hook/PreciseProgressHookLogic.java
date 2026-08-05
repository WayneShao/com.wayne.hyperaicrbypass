package com.wayne.hyperaicrbypass.hook;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class PreciseProgressHookLogic {
    private PreciseProgressHookLogic() {
    }

    public static Optional<PreciseProgressSnapshot> snapshotFromCalculator(
            Object[] args,
            Object result,
            long capturedElapsedRealtime
    ) {
        if (args == null || args.length != 8 || !(result instanceof Integer fixedProgress)) {
            return Optional.empty();
        }
        int[] values = new int[8];
        for (int index = 0; index < args.length; index++) {
            if (!(args[index] instanceof Integer value)) {
                return Optional.empty();
            }
            values[index] = value;
        }
        return PreciseProgressSnapshot.create(
                values[0], values[1], values[2], values[3],
                values[4], values[5], values[6], values[7],
                fixedProgress, capturedElapsedRealtime
        );
    }

    public static OptionalInt requiredInteger(Map<String, ?> values, String key) {
        if (values == null || !values.containsKey(key)) {
            return OptionalInt.empty();
        }
        Object value = values.get(key);
        return value instanceof Integer integer
                ? OptionalInt.of(integer)
                : OptionalInt.empty();
    }

    public static boolean shouldAttach(
            boolean enabled,
            int scope,
            Integer progress,
            PreciseProgressSnapshot snapshot,
            long nowElapsedRealtime
    ) {
        return enabled
                && scope == 1
                && progress != null
                && snapshot != null
                && snapshot.isCompatible(progress, nowElapsedRealtime);
    }
}

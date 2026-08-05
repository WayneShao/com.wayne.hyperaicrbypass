package com.wayne.hyperaicrbypass.hook;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public final class GlobalProgressMath {
    private GlobalProgressMath() {
    }

    public static Optional<GlobalProgressSnapshot> migratedDirect(
            GlobalProgressComponent gallery,
            GlobalProgressComponent note,
            GlobalProgressComponent message,
            GlobalProgressComponent file,
            GlobalProgressComponent record,
            int observedGlobalProgress,
            long runStartTime,
            long requestGeneration,
            long capturedElapsedRealtime
    ) {
        if (gallery == null || note == null || message == null
                || file == null || record == null) {
            return Optional.empty();
        }

        int localNative = (int) ((record.fixedProgress() * 0.25f)
                + (message.fixedProgress() * 0.25f)
                + (file.fixedProgress() * 0.25f)
                + (note.fixedProgress() * 0.25f));
        int globalNative = (int) ((gallery.fixedProgress() * 0.2f)
                + (localNative * 0.8f));
        if (globalNative != observedGlobalProgress) {
            return Optional.empty();
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (GlobalProgressComponent component :
                List.of(gallery, note, message, file, record)) {
            sum = sum.add(component.precisePercent());
        }
        int thousandths = sum.divide(BigDecimal.valueOf(5L), 3, RoundingMode.HALF_UP)
                .movePointRight(3)
                .intValueExact();
        try {
            return Optional.of(new GlobalProgressSnapshot(
                    thousandths,
                    observedGlobalProgress,
                    GlobalProgressBranch.MIGRATED_DIRECT_AI,
                    runStartTime,
                    requestGeneration,
                    capturedElapsedRealtime
            ));
        } catch (IllegalArgumentException | ArithmeticException error) {
            return Optional.empty();
        }
    }
}

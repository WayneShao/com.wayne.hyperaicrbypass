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

        int localNative = localNative(note, message, file, record);
        int globalNative = (int) ((gallery.fixedProgress() * 0.2f)
                + (localNative * 0.8f));
        if (globalNative != observedGlobalProgress) {
            return Optional.empty();
        }

        return snapshot(
                average(List.of(gallery, note, message, file, record)),
                observedGlobalProgress,
                GlobalProgressBranch.MIGRATED_DIRECT_AI,
                runStartTime,
                requestGeneration,
                capturedElapsedRealtime
        );
    }

    public static Optional<GlobalProgressSnapshot> unmigratedLocal(
            GlobalProgressComponent note,
            GlobalProgressComponent message,
            GlobalProgressComponent file,
            GlobalProgressComponent record,
            int observedGlobalProgress,
            long runStartTime,
            long requestGeneration,
            long capturedElapsedRealtime
    ) {
        if (note == null || message == null || file == null || record == null
                || localNative(note, message, file, record) != observedGlobalProgress) {
            return Optional.empty();
        }
        return snapshot(
                average(List.of(note, message, file, record)),
                observedGlobalProgress,
                GlobalProgressBranch.UNMIGRATED_LOCAL,
                runStartTime,
                requestGeneration,
                capturedElapsedRealtime
        );
    }

    public static Optional<GlobalProgressSnapshot> migratedPostprocessed(
            GlobalProgressComponent galleryAi,
            float galleryAppProgress,
            int migratedCount,
            int mediaCountBefore,
            int mediaCountCurrent,
            int observedGalleryProgress,
            GlobalProgressComponent note,
            GlobalProgressComponent message,
            GlobalProgressComponent file,
            GlobalProgressComponent record,
            int observedGlobalProgress,
            long runStartTime,
            long requestGeneration,
            long capturedElapsedRealtime
    ) {
        if (galleryAi == null || note == null || message == null
                || file == null || record == null) {
            return Optional.empty();
        }
        Optional<BigDecimal> galleryPrecise = postprocessedGallery(
                galleryAi,
                galleryAppProgress,
                migratedCount,
                mediaCountBefore,
                mediaCountCurrent,
                observedGalleryProgress
        );
        int localNative = localNative(note, message, file, record);
        int globalNative = (int) ((observedGalleryProgress * 0.2f)
                + (localNative * 0.8f));
        if (galleryPrecise.isEmpty() || globalNative != observedGlobalProgress) {
            return Optional.empty();
        }
        BigDecimal sum = galleryPrecise.get();
        for (GlobalProgressComponent component : List.of(note, message, file, record)) {
            sum = sum.add(component.precisePercent());
        }
        return snapshot(
                sum.divide(BigDecimal.valueOf(5L), 12, RoundingMode.HALF_UP),
                observedGlobalProgress,
                GlobalProgressBranch.MIGRATED_POSTPROCESSED,
                runStartTime,
                requestGeneration,
                capturedElapsedRealtime
        );
    }

    private static Optional<BigDecimal> postprocessedGallery(
            GlobalProgressComponent galleryAi,
            float galleryAppProgress,
            int migratedCount,
            int mediaCountBefore,
            int mediaCountCurrent,
            int observedGalleryProgress
    ) {
        if (mediaCountCurrent <= 0) {
            return observedGalleryProgress == 100
                    ? Optional.of(BigDecimal.valueOf(100L))
                    : Optional.empty();
        }
        if (!Float.isFinite(galleryAppProgress)
                || galleryAppProgress <= 0.0f
                || migratedCount < 0
                || mediaCountBefore <= 0) {
            return Optional.empty();
        }
        float migrationPart = galleryAppProgress / 10.0f;
        float galleryPart = galleryAppProgress - migrationPart;
        float before = mediaCountBefore;
        float migrationRatio = migratedCount
                / ((galleryAppProgress / 100.0f) * before);
        if (migrationRatio < 1.0f) {
            migrationPart *= migrationRatio;
        }
        float mediaRatio = mediaCountBefore < mediaCountCurrent
                ? before / mediaCountCurrent : 1.0f;
        float offset = (migrationPart * mediaRatio) + (galleryPart * mediaRatio);
        final int nativeGallery;
        try {
            nativeGallery = Math.min(100,
                    Math.addExact(galleryAi.fixedProgress(), (int) offset));
        } catch (ArithmeticException error) {
            return Optional.empty();
        }
        if (nativeGallery != observedGalleryProgress) {
            return Optional.empty();
        }
        BigDecimal precise = galleryAi.precisePercent()
                .add(BigDecimal.valueOf((double) offset))
                .max(BigDecimal.ZERO)
                .min(BigDecimal.valueOf(100L));
        return Optional.of(precise);
    }

    private static int localNative(
            GlobalProgressComponent note,
            GlobalProgressComponent message,
            GlobalProgressComponent file,
            GlobalProgressComponent record
    ) {
        return (int) ((record.fixedProgress() * 0.25f)
                + (message.fixedProgress() * 0.25f)
                + (file.fixedProgress() * 0.25f)
                + (note.fixedProgress() * 0.25f));
    }

    private static BigDecimal average(List<GlobalProgressComponent> components) {
        BigDecimal sum = BigDecimal.ZERO;
        for (GlobalProgressComponent component : components) {
            sum = sum.add(component.precisePercent());
        }
        return sum.divide(
                BigDecimal.valueOf(components.size()), 12, RoundingMode.HALF_UP
        );
    }

    private static Optional<GlobalProgressSnapshot> snapshot(
            BigDecimal precisePercent,
            int fixedProgress,
            GlobalProgressBranch branch,
            long runStartTime,
            long requestGeneration,
            long capturedElapsedRealtime
    ) {
        try {
            int thousandths = precisePercent
                    .max(BigDecimal.ZERO)
                    .min(BigDecimal.valueOf(100L))
                    .setScale(3, RoundingMode.HALF_UP)
                    .movePointRight(3)
                    .intValueExact();
            return Optional.of(new GlobalProgressSnapshot(
                    thousandths,
                    fixedProgress,
                    branch,
                    runStartTime,
                    requestGeneration,
                    capturedElapsedRealtime
            ));
        } catch (IllegalArgumentException | ArithmeticException error) {
            return Optional.empty();
        }
    }
}

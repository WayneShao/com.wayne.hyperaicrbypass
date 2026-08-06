package com.wayne.hyperaicrbypass.hook;

import com.wayne.hyperaicrbypass.config.ProgressPrecision;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GlobalProgressDisplay {
    private static final Pattern PERCENTAGE =
            Pattern.compile("[0-9]{1,3}(?:\\.[0-9]+)?%");

    private GlobalProgressDisplay() {
    }

    public static Optional<LoadingPlan> loadingPlan(
            int carrierScope,
            int analyseProgress,
            boolean initiativeStart,
            boolean initiativePause,
            boolean pausedByHandle
    ) {
        if (carrierScope == 31
                && analyseProgress < 0
                && initiativeStart
                && !initiativePause
                && !pausedByHandle) {
            return Optional.of(new LoadingPlan("...", false));
        }
        return Optional.empty();
    }

    public static String format(GlobalProgressSnapshot snapshot) {
        return format(snapshot, ProgressPrecision.THOUSANDTHS);
    }

    public static String format(
            GlobalProgressSnapshot snapshot,
            ProgressPrecision precision
    ) {
        return BigDecimal.valueOf(snapshot.thousandthsPercent(), 3)
                .setScale(precision.scale(), RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    public static Optional<RenderPlan> plan(
            CharSequence originalDescription,
            CharSequence originalButtonText,
            CharSequence originalContentDescription,
            int carrierScope,
            int analyseStatus,
            int analyseProgress,
            GlobalProgressSnapshot snapshot,
            long currentRunStartTime,
            long nowElapsedRealtime
    ) {
        return plan(originalDescription, originalButtonText, originalContentDescription,
                carrierScope, analyseStatus, analyseProgress, snapshot, currentRunStartTime,
                nowElapsedRealtime, ProgressPrecision.THOUSANDTHS);
    }

    public static Optional<RenderPlan> plan(
            CharSequence originalDescription,
            CharSequence originalButtonText,
            CharSequence originalContentDescription,
            int carrierScope,
            int analyseStatus,
            int analyseProgress,
            GlobalProgressSnapshot snapshot,
            long currentRunStartTime,
            long nowElapsedRealtime,
            ProgressPrecision precision
    ) {
        if (carrierScope != 31
                || precision == null
                || !precision.isPrecise()
                || analyseStatus < 0
                || analyseProgress >= 100
                || snapshot == null
                || originalDescription == null
                || originalButtonText == null
                || originalContentDescription == null
                || !snapshot.isDisplayCompatible(
                        analyseProgress, currentRunStartTime, nowElapsedRealtime)) {
            return Optional.empty();
        }
        String nativeToken = analyseProgress + "%";
        String description = originalDescription.toString();
        String button = originalButtonText.toString();
        String contentDescription = originalContentDescription.toString();
        String precise = format(snapshot, precision);
        String renderedDescription = nativeToken.equals(firstPercentage(description))
                ? replaceFirstPercentage(description, precise) : description;
        String renderedButton = nativeToken.equals(button) ? precise : button;
        String renderedContentDescription = nativeToken.equals(contentDescription)
                ? precise : contentDescription;
        if (renderedDescription.equals(description)
                && renderedButton.equals(button)
                && renderedContentDescription.equals(contentDescription)) {
            return Optional.empty();
        }
        return Optional.of(new RenderPlan(
                renderedDescription,
                renderedButton,
                renderedContentDescription
        ));
    }

    private static String firstPercentage(String value) {
        Matcher matcher = PERCENTAGE.matcher(value);
        return matcher.find() ? matcher.group() : null;
    }

    private static String replaceFirstPercentage(String original, String replacement) {
        Matcher matcher = PERCENTAGE.matcher(original);
        if (!matcher.find()) {
            return original;
        }
        return original.substring(0, matcher.start())
                + replacement
                + original.substring(matcher.end());
    }

    public record RenderPlan(
            String description,
            String buttonText,
            String contentDescription
    ) {
    }

    public record LoadingPlan(String buttonText, boolean enabled) {
    }
}

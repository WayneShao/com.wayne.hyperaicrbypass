package com.wayne.hyperaicrbypass.hook;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GlobalProgressDisplay {
    private static final Pattern PERCENTAGE =
            Pattern.compile("[0-9]{1,3}(?:\\.[0-9]+)?%");

    private GlobalProgressDisplay() {
    }

    public static String format(GlobalProgressSnapshot snapshot) {
        int whole = snapshot.thousandthsPercent() / 1_000;
        int fraction = snapshot.thousandthsPercent() % 1_000;
        return String.format(Locale.ROOT, "%d.%03d%%", whole, fraction);
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
        if (carrierScope != 31
                || analyseStatus <= 0
                || snapshot == null
                || originalDescription == null
                || originalButtonText == null
                || originalContentDescription == null
                || !snapshot.isCompatible(
                        analyseProgress, currentRunStartTime, nowElapsedRealtime)) {
            return Optional.empty();
        }
        String nativeToken = analyseProgress + "%";
        String description = originalDescription.toString();
        String button = originalButtonText.toString();
        String contentDescription = originalContentDescription.toString();
        if (!nativeToken.equals(firstPercentage(description))
                || !nativeToken.equals(button)
                || !nativeToken.equals(contentDescription)) {
            return Optional.empty();
        }
        String precise = format(snapshot);
        return Optional.of(new RenderPlan(
                replaceFirstPercentage(description, precise),
                precise,
                precise
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
}

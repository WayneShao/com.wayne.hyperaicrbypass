package com.wayne.hyperaicrbypass.hook;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PreciseProgressDisplay {
    private static final String GALLERY_PACKAGE = "com.miui.gallery";
    private static final BigDecimal ZERO = new BigDecimal("0.000");
    private static final BigDecimal HUNDRED = new BigDecimal("100.000");
    private static final Pattern PERCENTAGE =
            Pattern.compile("[0-9]{1,3}(?:\\.[0-9]+)?%");

    private PreciseProgressDisplay() {
    }

    public static String format(PreciseProgressSnapshot snapshot) {
        Objects.requireNonNull(snapshot);
        if (snapshot.isDenominatorlessCompletion()) {
            return "100.000%";
        }
        BigDecimal percentage = BigDecimal.valueOf(snapshot.numerator())
                .multiply(BigDecimal.valueOf(100L))
                .divide(BigDecimal.valueOf(snapshot.denominator()), 3, RoundingMode.HALF_UP);
        if (percentage.compareTo(ZERO) < 0) {
            percentage = ZERO;
        } else if (percentage.compareTo(HUNDRED) > 0) {
            percentage = HUNDRED;
        }
        return percentage.setScale(3, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    public static String replaceFirstPercentage(String original, String replacement) {
        Objects.requireNonNull(original);
        Objects.requireNonNull(replacement);
        Matcher matcher = PERCENTAGE.matcher(original);
        if (!matcher.find()) {
            return original;
        }
        return original.substring(0, matcher.start())
                + replacement
                + original.substring(matcher.end());
    }

    public static CharSequence render(
            CharSequence original,
            String scopePackage,
            int uiProgress,
            PreciseProgressSnapshot snapshot,
            long nowElapsedRealtime
    ) {
        if (original == null
                || !GALLERY_PACKAGE.equals(scopePackage)
                || snapshot == null
                || !snapshot.isCompatible(uiProgress, nowElapsedRealtime)) {
            return original;
        }
        return replaceFirstPercentage(original.toString(), format(snapshot));
    }
}

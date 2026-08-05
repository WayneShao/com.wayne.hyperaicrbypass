package com.wayne.hyperaicrbypass.hook;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public record GlobalProgressComponent(
        long numerator,
        long denominator,
        int fixedProgress
) {
    public GlobalProgressComponent {
        if (numerator < 0L
                || denominator < 0L
                || (denominator == 0L && (numerator != 0L || fixedProgress != 100))
                || fixedProgress < 0
                || fixedProgress > 100) {
            throw new IllegalArgumentException("Invalid global progress component");
        }
    }

    public static Optional<GlobalProgressComponent> restore(
            long numerator,
            long denominator,
            int fixedProgress
    ) {
        try {
            return Optional.of(new GlobalProgressComponent(
                    numerator, denominator, fixedProgress
            ));
        } catch (IllegalArgumentException error) {
            return Optional.empty();
        }
    }

    public static Optional<GlobalProgressComponent> fromLocalCounts(
            int original,
            int inverted,
            int vector,
            int fixedProgress
    ) {
        if (original < 0 || inverted < 0 || vector < 0) {
            return Optional.empty();
        }
        try {
            long numerator = Math.addExact(
                    (long) inverted,
                    Math.multiplyExact((long) vector, 2L)
            );
            long denominator = Math.multiplyExact((long) original, 2L);
            return denominator == 0L
                    ? restore(0L, 0L, fixedProgress)
                    : restore(numerator, denominator, fixedProgress);
        } catch (ArithmeticException error) {
            return Optional.empty();
        }
    }

    BigDecimal precisePercent() {
        if (denominator == 0L) {
            return BigDecimal.valueOf(100L);
        }
        BigDecimal value = BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100L))
                .divide(BigDecimal.valueOf(denominator), 12, RoundingMode.HALF_UP);
        return value.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100L));
    }
}

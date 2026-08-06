package com.wayne.hyperaicrbypass.config;

public enum ProgressPrecision {
    ORIGINAL(0),
    TENTHS(1),
    HUNDREDTHS(2),
    THOUSANDTHS(3);

    private final int scale;

    ProgressPrecision(int scale) {
        this.scale = scale;
    }

    public int scale() {
        return scale;
    }

    public boolean isPrecise() {
        return this != ORIGINAL;
    }

    public static ProgressPrecision fromStored(String value) {
        return ProgressPrecision.valueOf(value);
    }
}

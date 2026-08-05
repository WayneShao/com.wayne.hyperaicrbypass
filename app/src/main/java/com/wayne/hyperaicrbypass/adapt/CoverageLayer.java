package com.wayne.hyperaicrbypass.adapt;

public enum CoverageLayer {
    PENDING,
    EXACT,
    SEMANTIC,
    FALLBACK,
    PARTIAL,
    UNAVAILABLE;

    public boolean isSuccessful() {
        return this == EXACT || this == SEMANTIC || this == FALLBACK || this == PARTIAL;
    }
}

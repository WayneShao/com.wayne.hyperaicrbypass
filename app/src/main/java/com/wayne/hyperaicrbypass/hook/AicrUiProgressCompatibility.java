package com.wayne.hyperaicrbypass.hook;

public final class AicrUiProgressCompatibility {
    private AicrUiProgressCompatibility() {
    }

    public static Result normalize(
            boolean enabled,
            int progress,
            boolean hasSupport,
            int support,
            boolean hasInProgress,
            int inProgress
    ) {
        if (!enabled) {
            return new Result(hasSupport, support, hasInProgress, inProgress, false);
        }
        int normalizedState = progress == 100 ? 0 : 2;
        boolean changed = !hasSupport || support != 1
                || !hasInProgress || inProgress != normalizedState;
        return new Result(true, 1, true, normalizedState, changed);
    }

    public record Result(
            boolean hasSupport,
            int support,
            boolean hasInProgress,
            int inProgress,
            boolean changed
    ) {
    }
}

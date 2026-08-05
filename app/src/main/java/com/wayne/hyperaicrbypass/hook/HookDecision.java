package com.wayne.hyperaicrbypass.hook;

import com.wayne.hyperaicrbypass.config.BypassConfig;

public final class HookDecision {
    private HookDecision() {
    }

    public static Object result(BypassConfig config, HookSpec spec, Object original) {
        if (!config.shouldBypass(spec.policy())) {
            return original;
        }
        return switch (spec.behavior()) {
            case RESULT_TRUE -> Boolean.TRUE;
            case RESULT_FALSE -> Boolean.FALSE;
            case RESULT_ZERO_INT -> 0;
            case RESULT_ONE_INT -> 1;
            case RESULT_HUNDRED_INT -> 100;
            case RESULT_ZERO_LONG -> 0L;
            case ARGUMENT_ZERO_BOOLEAN, ARGUMENT_NOW_LONG -> original;
        };
    }

    public static boolean booleanArgument(
            BypassConfig config,
            HookSpec spec,
            boolean original
    ) {
        if (!config.shouldBypass(spec.policy())) {
            return original;
        }
        return spec.behavior() == HookBehavior.ARGUMENT_ZERO_BOOLEAN ? false : original;
    }

    public static long longArgument(
            BypassConfig config,
            HookSpec spec,
            long original,
            long now
    ) {
        if (!config.shouldBypass(spec.policy())) {
            return original;
        }
        return spec.behavior() == HookBehavior.ARGUMENT_NOW_LONG ? now : original;
    }
}

package com.wayne.hyperaicrbypass.hook;

public enum HookBehavior {
    RESULT_TRUE(true),
    RESULT_FALSE(true),
    RESULT_ZERO_INT(true),
    RESULT_ONE_INT(true),
    RESULT_HUNDRED_INT(true),
    RESULT_ZERO_LONG(true),
    ARGUMENT_ZERO_BOOLEAN(false),
    ARGUMENT_NOW_LONG(false);

    private final boolean changesResult;

    HookBehavior(boolean changesResult) {
        this.changesResult = changesResult;
    }

    public boolean changesResult() {
        return changesResult;
    }
}

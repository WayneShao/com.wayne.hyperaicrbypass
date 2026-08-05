package com.wayne.hyperaicrbypass.config;

public enum Policy {
    TEMPERATURE("temperature"),
    CHARGING("charging"),
    POWER("power"),
    SCREEN_IDLE("screen_idle"),
    MIGRATION("migration"),
    DAILY_COUNT("daily_count"),
    DURATION("duration"),
    RUN_GAP("run_gap"),
    OVERLOAD("overload"),
    TASK_CONSTRAINTS("task_constraints"),
    AI_UI_CAPABILITY("ai_ui_capability");

    private final String key;

    Policy(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static Policy fromKey(String key) {
        for (Policy policy : values()) {
            if (policy.key.equals(key)) {
                return policy;
            }
        }
        throw new IllegalArgumentException("Unknown policy key: " + key);
    }
}

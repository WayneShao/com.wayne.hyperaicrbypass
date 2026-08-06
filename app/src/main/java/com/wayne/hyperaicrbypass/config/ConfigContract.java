package com.wayne.hyperaicrbypass.config;

import java.util.Objects;

public final class ConfigContract {
    public static final String AUTHORITY = "com.wayne.hyperaicrbypass.settings";

    public static final String METHOD_GET_SNAPSHOT = "get_snapshot";
    public static final String METHOD_SET_MASTER = "set_master";
    public static final String METHOD_SET_MODE = "set_mode";
    public static final String METHOD_SET_POWER_EXCEPTION = "set_power_exception";
    public static final String METHOD_SET_PROGRESS_PRECISION = "set_progress_precision";
    public static final String METHOD_SET_POLICY = "set_policy";
    public static final String METHOD_SET_ALL = "set_all";
    public static final String METHOD_RESCAN = "rescan";
    public static final String METHOD_REPORT_COVERAGE = "report_coverage";
    public static final String METHOD_GET_COVERAGE = "get_coverage";

    public static final String KEY_MASTER = "master";
    public static final String KEY_MODE = "mode";
    public static final String KEY_POWER_EXCEPTION = "power_exception";
    public static final String KEY_PROGRESS_PRECISION = "progress_precision";
    public static final String KEY_LAST_NON_ORIGINAL_PRECISION = "last_non_original_precision";
    public static final String KEY_SELECT_ALL_MODE = "select_all_mode";
    public static final String KEY_CONFIG_REVISION = "config_revision";
    public static final String KEY_RESCAN_GENERATION = "rescan_generation";
    public static final String KEY_POLICY = "policy";
    public static final String KEY_SELECTED = "selected";
    public static final String KEY_POLICIES = "policies";
    public static final String KEY_AICR_VERSION = "aicr_version";
    public static final String KEY_PROCESS = "process";
    public static final String KEY_COVERAGE = "coverage";
    public static final String KEY_LAYER = "layer";
    public static final String KEY_GENERATION = "generation";
    public static final String POLICY_PREFIX = "policy.";

    public static final int MAX_SHORT_TEXT_LENGTH = 512;

    private ConfigContract() {
    }

    public static String policyKey(Policy policy) {
        return POLICY_PREFIX + Objects.requireNonNull(policy).getKey();
    }

    public static String coverageKey(Policy policy) {
        return KEY_COVERAGE + "." + Objects.requireNonNull(policy).getKey();
    }

    public static String requireShortText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.length() > MAX_SHORT_TEXT_LENGTH) {
            throw new IllegalArgumentException(fieldName + " exceeds " + MAX_SHORT_TEXT_LENGTH + " characters");
        }
        return value;
    }
}

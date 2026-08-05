package com.wayne.hyperaicrbypass.hook;

import com.wayne.hyperaicrbypass.config.BypassConfig;
import com.wayne.hyperaicrbypass.config.Policy;

public final class RunningStatusCompatibility {
    static final String RUNNING_STATUS_KEY = "runningStatus";
    static final int OVER_TEMPERATURE_STATUS = -2;

    private RunningStatusCompatibility() {
    }

    public static int normalize(BypassConfig config, String key, int value) {
        if (config.shouldBypass(Policy.TEMPERATURE)
                && RUNNING_STATUS_KEY.equals(key)
                && value == OVER_TEMPERATURE_STATUS) {
            return 0;
        }
        return value;
    }
}

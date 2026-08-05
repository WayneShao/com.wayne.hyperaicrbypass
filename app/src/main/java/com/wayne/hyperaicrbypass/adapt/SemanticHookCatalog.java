package com.wayne.hyperaicrbypass.adapt;

import static com.wayne.hyperaicrbypass.hook.HookBehavior.ARGUMENT_NOW_LONG;
import static com.wayne.hyperaicrbypass.hook.HookBehavior.RESULT_FALSE;
import static com.wayne.hyperaicrbypass.hook.HookBehavior.RESULT_TRUE;
import static com.wayne.hyperaicrbypass.hook.HookBehavior.RESULT_HUNDRED_INT;
import static com.wayne.hyperaicrbypass.hook.HookBehavior.RESULT_ONE_INT;
import static com.wayne.hyperaicrbypass.hook.HookBehavior.RESULT_ZERO_INT;
import static com.wayne.hyperaicrbypass.hook.HookBehavior.RESULT_ZERO_LONG;

import com.wayne.hyperaicrbypass.config.Policy;

import java.util.List;
import java.util.Set;

public final class SemanticHookCatalog {
    private static final List<SemanticHookSpec> SPECS = List.of(
            spec(Policy.TEMPERATURE, "getTemperature", "int", List.of(), false,
                    Set.of("board_sensor_charge_temp", "board_sensor_temp"), RESULT_ZERO_INT),
            spec(Policy.TEMPERATURE, "checkOverStartTemperatureLimit", "boolean",
                    List.of("int"), false, Set.of("temperatureLimit:", "temperature:"), RESULT_FALSE),
            spec(Policy.TEMPERATURE, "checkOverStopTemperatureLimit", "boolean",
                    List.of("int"), false, Set.of("temperatureLimit:", "temperature:"), RESULT_FALSE),
            spec(Policy.CHARGING, "getCharging", "int", List.of(), false,
                    Set.of("getCharging error"), RESULT_ONE_INT),
            spec(Policy.POWER, "getPower", "int", List.of(), false,
                    Set.of("getPower error"), RESULT_HUNDRED_INT),
            spec(Policy.SCREEN_IDLE, "getInteractive", "int", List.of(), false,
                    Set.of("getInteractive error"), RESULT_ZERO_INT),
            spec(Policy.MIGRATION, "getHuanji", "int", List.of(), false,
                    Set.of("HUANJI"), RESULT_ZERO_INT),
            spec(Policy.DAILY_COUNT, "getDailyRunningCount", "int", List.of(), false,
                    Set.of("超过当日建库次数"), RESULT_ZERO_INT),
            spec(Policy.DURATION, "canStop", "boolean", List.of("long"), false,
                    Set.of("canStop-不满足建库条件 duration:"), ARGUMENT_NOW_LONG),
            spec(Policy.RUN_GAP, "getRunningGapTime", "long", List.of(), false,
                    Set.of("getGap error:"), RESULT_ZERO_LONG),
            spec(Policy.OVERLOAD, "checkIsOverloadScene", "boolean",
                    List.of("android.content.Context", "int"), true,
                    Set.of("checkIsOverloadScene fail"), RESULT_FALSE),
            spec(Policy.AI_UI_CAPABILITY, "isSupportAISearchUIV2", "boolean",
                    List.of(), true,
                    Set.of("is_support_ai_search_progress", "O1"), RESULT_TRUE)
    );

    private SemanticHookCatalog() {
    }

    public static List<SemanticHookSpec> specs() {
        return SPECS;
    }

    private static SemanticHookSpec spec(
            Policy policy,
            String method,
            String returnType,
            List<String> parameters,
            boolean isStatic,
            Set<String> anchors,
            com.wayne.hyperaicrbypass.hook.HookBehavior behavior
    ) {
        return new SemanticHookSpec(
                policy, method, returnType, parameters, isStatic, anchors, behavior
        );
    }
}

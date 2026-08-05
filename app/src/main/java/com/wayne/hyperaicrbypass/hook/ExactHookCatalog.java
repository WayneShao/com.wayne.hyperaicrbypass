package com.wayne.hyperaicrbypass.hook;

import com.wayne.hyperaicrbypass.config.Policy;

import java.util.List;

public final class ExactHookCatalog {
    private static final String RUNNING_STATUS =
            "com.xiaomi.aicr.searchpro.monitor.RunningStatus";
    private static final String RUN_LEVEL =
            "com.xiaomi.aicr.searchpro.monitor.RunLevel";
    private static final String STATUS_BEAN =
            "com.xiaomi.aicr.searchpro.monitor.StatusBean";

    private static final List<HookSpec> AICR_SPECS = List.of(
            spec(RUNNING_STATUS, "getTemperature", "int", Policy.TEMPERATURE,
                    HookBehavior.RESULT_ZERO_INT),
            spec(RUNNING_STATUS, "checkOverStartTemperatureLimit", "boolean",
                    List.of("int"), Policy.TEMPERATURE, HookBehavior.RESULT_FALSE),
            spec(RUNNING_STATUS, "checkOverStopTemperatureLimit", "boolean",
                    List.of("int"), Policy.TEMPERATURE, HookBehavior.RESULT_FALSE),
            spec(RUNNING_STATUS, "getCharging", "int", Policy.CHARGING,
                    HookBehavior.RESULT_ONE_INT),
            spec(RUNNING_STATUS, "getPower", "int", Policy.POWER,
                    HookBehavior.RESULT_HUNDRED_INT),
            spec(RUNNING_STATUS, "getInteractive", "int", Policy.SCREEN_IDLE,
                    HookBehavior.RESULT_ZERO_INT),
            spec(RUNNING_STATUS, "getHuanji", "int", Policy.MIGRATION,
                    HookBehavior.RESULT_ZERO_INT),
            spec(RUNNING_STATUS, "getDailyRunningCount", "int", Policy.DAILY_COUNT,
                    HookBehavior.RESULT_ZERO_INT),
            spec(RUN_LEVEL, "canStop", "boolean", List.of("long"), Policy.DURATION,
                    HookBehavior.ARGUMENT_NOW_LONG),
            spec(STATUS_BEAN, "canStop", "boolean", List.of("long"), Policy.DURATION,
                    HookBehavior.ARGUMENT_NOW_LONG),
            spec(RUNNING_STATUS, "getRunningGapTime", "long", Policy.RUN_GAP,
                    HookBehavior.RESULT_ZERO_LONG),
            spec("com.xiaomi.aicr.common.OverloadSceneUtil", "checkIsOverloadScene",
                    "boolean", List.of("android.content.Context", "int"), Policy.OVERLOAD,
                    HookBehavior.RESULT_FALSE),
            spec("com.xiaomi.aicr.common.StatusUtils", "isSupportAISearchUIV2",
                    "boolean", Policy.AI_UI_CAPABILITY, HookBehavior.RESULT_TRUE),
            spec("android.app.job.JobInfo$Builder", "setRequiresCharging",
                    "android.app.job.JobInfo$Builder", List.of("boolean"),
                    Policy.TASK_CONSTRAINTS, HookBehavior.ARGUMENT_ZERO_BOOLEAN),
            spec("android.app.job.JobInfo$Builder", "setRequiresBatteryNotLow",
                    "android.app.job.JobInfo$Builder", List.of("boolean"),
                    Policy.TASK_CONSTRAINTS, HookBehavior.ARGUMENT_ZERO_BOOLEAN),
            spec("android.app.job.JobInfo$Builder", "setRequiresDeviceIdle",
                    "android.app.job.JobInfo$Builder", List.of("boolean"),
                    Policy.TASK_CONSTRAINTS, HookBehavior.ARGUMENT_ZERO_BOOLEAN)
    );

    private ExactHookCatalog() {
    }

    public static List<HookSpec> aicrSpecs() {
        return AICR_SPECS;
    }

    private static HookSpec spec(
            String className,
            String methodName,
            String returnType,
            Policy policy,
            HookBehavior behavior
    ) {
        return spec(className, methodName, returnType, List.of(), policy, behavior);
    }

    private static HookSpec spec(
            String className,
            String methodName,
            String returnType,
            List<String> parameterTypes,
            Policy policy,
            HookBehavior behavior
    ) {
        return new HookSpec(className, methodName, returnType, parameterTypes, policy, behavior);
    }
}

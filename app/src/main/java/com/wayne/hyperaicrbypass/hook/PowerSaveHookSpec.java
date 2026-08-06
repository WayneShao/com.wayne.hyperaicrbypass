package com.wayne.hyperaicrbypass.hook;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PowerSaveHookSpec(
        String className,
        String methodName,
        String returnType,
        List<String> parameterTypes,
        Set<String> requiredAnchors,
        boolean pauseResult
) {
    private static final String RUNNING_STATUS =
            "com.xiaomi.aicr.searchpro.monitor.RunningStatus";
    private static final List<PowerSaveHookSpec> CATALOG = List.of(
            new PowerSaveHookSpec(
                    RUNNING_STATUS,
                    "checkCanStart",
                    "boolean",
                    List.of("int"),
                    Set.of("checkCanStart error:", "no cloud start config"),
                    false
            ),
            new PowerSaveHookSpec(
                    RUNNING_STATUS,
                    "getNeedStop",
                    "boolean",
                    List.of(),
                    Set.of("getNeedStop canStop:",
                            "running status -> RUNNING_LEVEL_STOP(0)"),
                    true
            )
    );

    public PowerSaveHookSpec {
        Objects.requireNonNull(className);
        Objects.requireNonNull(methodName);
        Objects.requireNonNull(returnType);
        parameterTypes = List.copyOf(parameterTypes);
        requiredAnchors = Set.copyOf(requiredAnchors);
    }

    public static List<PowerSaveHookSpec> catalog() {
        return CATALOG;
    }
}

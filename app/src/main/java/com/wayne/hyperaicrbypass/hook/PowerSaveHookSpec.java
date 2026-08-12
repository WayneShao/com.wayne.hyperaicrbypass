package com.wayne.hyperaicrbypass.hook;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PowerSaveHookSpec(
        Boundary boundary,
        String className,
        String methodName,
        String returnType,
        List<String> parameterTypes,
        Set<String> requiredAnchors,
        boolean pauseResult,
        boolean allowStatic
) {
    private static final String RUNNING_STATUS =
            "com.xiaomi.aicr.searchpro.monitor.RunningStatus";
    private static final List<PowerSaveHookSpec> CATALOG = List.of(
            new PowerSaveHookSpec(
                    Boundary.START,
                    RUNNING_STATUS,
                    "checkCanStart",
                    "boolean",
                    List.of("int"),
                    Set.of("checkCanStart error:", "no cloud start config"),
                    false,
                    false
            ),
            new PowerSaveHookSpec(
                    Boundary.STOP,
                    RUNNING_STATUS,
                    "getNeedStop",
                    "boolean",
                    List.of(),
                    Set.of("getNeedStop canStop:",
                            "running status -> RUNNING_LEVEL_STOP(0)"),
                    true,
                    false
            )
    );
    private static final List<PowerSaveHookSpec> VERSION_3_63_CATALOG = List.of(
            new PowerSaveHookSpec(
                    Boundary.START,
                    "u16",
                    "d",
                    "boolean",
                    List.of("int"),
                    Set.of("RunningStatus.checkCanStart", "no cloud start config"),
                    false,
                    true
            ),
            new PowerSaveHookSpec(
                    Boundary.STOP,
                    "u16",
                    "q",
                    "boolean",
                    List.of(),
                    Set.of("RunningStatus.getNeedStop", "getNeedStop canStop:"),
                    true,
                    false
            )
    );

    public PowerSaveHookSpec {
        Objects.requireNonNull(boundary);
        Objects.requireNonNull(className);
        Objects.requireNonNull(methodName);
        Objects.requireNonNull(returnType);
        parameterTypes = List.copyOf(parameterTypes);
        requiredAnchors = Set.copyOf(requiredAnchors);
    }

    public static List<PowerSaveHookSpec> catalog() {
        return CATALOG;
    }

    public static List<PowerSaveHookSpec> catalog(AicrVersionBranch branch) {
        return switch (branch) {
            case V3 -> VERSION_3_63_CATALOG;
            case V4 -> CATALOG;
            case UNKNOWN -> {
                java.util.ArrayList<PowerSaveHookSpec> result =
                        new java.util.ArrayList<>(VERSION_3_63_CATALOG);
                result.addAll(CATALOG);
                yield List.copyOf(result);
            }
        };
    }

    public enum Boundary {
        START,
        STOP
    }
}

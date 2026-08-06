package com.wayne.hyperaicrbypass.hook;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AicrProviderHookSpec(
        Role role,
        String className,
        String returnType,
        List<String> parameterTypes,
        Set<String> requiredAnchors
) {
    public enum Role {
        DATABASE,
        UI,
        NLS
    }

    private static final List<String> CALL_PARAMETERS = List.of(
            "java.lang.String", "java.lang.String", "android.os.Bundle"
    );
    private static final List<AicrProviderHookSpec> CRITICAL_CATALOG = List.of(
            new AicrProviderHookSpec(
                    Role.DATABASE,
                    "com.xiaomi.aicr.aisearch.provider.SearchDataBaseProvider",
                    "android.os.Bundle",
                    CALL_PARAMETERS,
                    Set.of(
                            "method_algo_analyse_start",
                            "method_algo_analyse_UNLIMITED",
                            "method_algo_analyse_stop",
                            "method_algo_analyse_finish"
                    )
            ),
            new AicrProviderHookSpec(
                    Role.UI,
                    "com.xiaomi.aicr.aisearch.AISearchUIProvider",
                    "android.os.Bundle",
                    CALL_PARAMETERS,
                    Set.of("method_change_algo_state", "is_run_algo", "analyse_progress")
            )
    );

    public AicrProviderHookSpec {
        Objects.requireNonNull(role);
        Objects.requireNonNull(className);
        Objects.requireNonNull(returnType);
        parameterTypes = List.copyOf(parameterTypes);
        requiredAnchors = Set.copyOf(requiredAnchors);
    }

    public static List<AicrProviderHookSpec> criticalCatalog() {
        return CRITICAL_CATALOG;
    }
}

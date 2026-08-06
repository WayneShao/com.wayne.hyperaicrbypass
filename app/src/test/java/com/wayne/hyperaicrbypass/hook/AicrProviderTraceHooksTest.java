package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class AicrProviderTraceHooksTest {
    @Test
    public void pauseBlocksOnlyDatabaseStartMethods() {
        assertEquals(AicrProviderTraceHooks.Action.BLOCK_DATABASE_START,
                action(AicrProviderHookSpec.Role.DATABASE,
                        "method_algo_analyse_start", false, true));
        assertEquals(AicrProviderTraceHooks.Action.BLOCK_DATABASE_START,
                action(AicrProviderHookSpec.Role.DATABASE,
                        "method_algo_analyse_UNLIMITED", false, true));
        assertEquals(AicrProviderTraceHooks.Action.ALLOW,
                action(AicrProviderHookSpec.Role.DATABASE,
                        "method_algo_analyse_stop", false, true));
        assertEquals(AicrProviderTraceHooks.Action.ALLOW,
                action(AicrProviderHookSpec.Role.DATABASE,
                        "method_algo_analyse_finish", false, true));
        assertEquals(AicrProviderTraceHooks.Action.ALLOW,
                action(AicrProviderHookSpec.Role.DATABASE,
                        "method_algo_get_progress", false, true));
    }

    @Test
    public void pauseTurnsOnlyRequestedUiStartIntoPausedState() {
        assertEquals(AicrProviderTraceHooks.Action.CONVERT_UI_START_TO_PAUSE,
                action(AicrProviderHookSpec.Role.UI,
                        "method_change_algo_state", true, true));
        assertEquals(AicrProviderTraceHooks.Action.ALLOW,
                action(AicrProviderHookSpec.Role.UI,
                        "method_change_algo_state", false, true));
        assertEquals(AicrProviderTraceHooks.Action.ALLOW,
                action(AicrProviderHookSpec.Role.UI,
                        "method_algo_get_progress", true, true));
        assertEquals(AicrProviderTraceHooks.Action.ALLOW,
                action(AicrProviderHookSpec.Role.UI,
                        "method_change_algo_state", true, false));
    }

    @Test
    public void semanticProviderSpecsUseFullCallShapeAndSeparateAnchors() {
        List<AicrProviderHookSpec> specs = AicrProviderHookSpec.criticalCatalog();
        assertEquals(2, specs.size());
        for (AicrProviderHookSpec spec : specs) {
            assertEquals("android.os.Bundle", spec.returnType());
            assertEquals(List.of("java.lang.String", "java.lang.String", "android.os.Bundle"),
                    spec.parameterTypes());
        }
        AicrProviderHookSpec database = specs.stream()
                .filter(spec -> spec.role() == AicrProviderHookSpec.Role.DATABASE)
                .findFirst().orElseThrow();
        AicrProviderHookSpec ui = specs.stream()
                .filter(spec -> spec.role() == AicrProviderHookSpec.Role.UI)
                .findFirst().orElseThrow();
        assertTrue(database.requiredAnchors().contains("method_algo_analyse_UNLIMITED"));
        assertTrue(database.requiredAnchors().contains("method_algo_analyse_stop"));
        assertTrue(ui.requiredAnchors().contains("method_change_algo_state"));
        assertTrue(ui.requiredAnchors().contains("is_run_algo"));
    }

    private static AicrProviderTraceHooks.Action action(
            AicrProviderHookSpec.Role role,
            String method,
            boolean isRunAlgo,
            boolean shouldPause
    ) {
        return AicrProviderTraceHooks.decide(role, method, isRunAlgo, shouldPause);
    }
}

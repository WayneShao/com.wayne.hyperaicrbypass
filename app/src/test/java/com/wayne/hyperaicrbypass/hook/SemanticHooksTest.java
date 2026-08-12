package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wayne.hyperaicrbypass.config.Policy;

import org.junit.Test;

import java.util.List;

public final class SemanticHooksTest {
    @Test
    public void skipsDiscoveryWhenNoExactHookIsMissing() {
        assertFalse(SemanticDiscoveryPolicy.needsDiscovery(List.of()));
    }

    @Test
    public void skipsDiscoveryForFrameworkOnlyMisses() {
        HookSpec frameworkHook = new HookSpec(
                "android.app.job.JobInfo$Builder",
                "setRequiresCharging",
                "android.app.job.JobInfo$Builder",
                List.of("boolean"),
                Policy.TASK_CONSTRAINTS,
                HookBehavior.ARGUMENT_ZERO_BOOLEAN
        );

        assertFalse(SemanticDiscoveryPolicy.needsDiscovery(List.of(frameworkHook)));
    }

    @Test
    public void enablesDiscoveryForAicrMisses() {
        HookSpec aicrHook = new HookSpec(
                "com.xiaomi.aicr.search.core.status.RunningStatus",
                "getTemperature",
                "int",
                List.of(),
                Policy.TEMPERATURE,
                HookBehavior.RESULT_ZERO_INT
        );

        assertTrue(SemanticDiscoveryPolicy.needsDiscovery(List.of(aicrHook)));
    }

    @Test
    public void enablesDiscoveryForObfuscatedAicrMissesByPolicy() {
        HookSpec obfuscated = new HookSpec(
                "u16",
                "C",
                "int",
                List.of(),
                Policy.TEMPERATURE,
                HookBehavior.RESULT_ZERO_INT
        );

        assertTrue(SemanticDiscoveryPolicy.needsDiscovery(List.of(obfuscated)));
        assertTrue(SemanticHooks.semanticSpecsFor(List.of(obfuscated)).stream()
                .anyMatch(spec -> spec.policy() == Policy.TEMPERATURE));
    }

    @Test
    public void unknownBranchScansAllSemanticPoliciesWithoutFakeExactMisses() {
        assertTrue(SemanticHooks.semanticSpecsFor(
                List.of(), AicrVersionBranch.UNKNOWN
        ).size() >= Policy.values().length - 1);
    }
}

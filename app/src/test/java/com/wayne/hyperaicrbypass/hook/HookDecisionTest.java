package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.wayne.hyperaicrbypass.config.BypassConfig;
import com.wayne.hyperaicrbypass.config.Policy;

import org.junit.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class HookDecisionTest {
    private static final Set<String> FORBIDDEN_METHODS = Set.of(
            "checkCanStart", "checkCanStop", "getNeedStop", "setRunningStatus"
    );

    @Test
    public void catalogCoversEveryPolicyWithSingleOwnerSpecs() {
        List<HookSpec> specs = ExactHookCatalog.aicrSpecs();
        EnumSet<Policy> covered = EnumSet.noneOf(Policy.class);

        for (HookSpec spec : specs) {
            covered.add(spec.policy());
            assertFalse(spec.className().isBlank());
            assertFalse(spec.methodName().isBlank());
            assertFalse(FORBIDDEN_METHODS.contains(spec.methodName()));
            assertEquals(spec.parameterTypes().size(), spec.parameterCount());
        }

        assertEquals(EnumSet.allOf(Policy.class), covered);
    }

    @Test
    public void disablingOnePolicyRestoresOnlyItsOriginalResult() {
        for (Policy disabled : Policy.values()) {
            BypassConfig config = BypassConfig.defaults().withPolicy(disabled, false);
            for (HookSpec spec : ExactHookCatalog.aicrSpecs()) {
                if (!spec.behavior().changesResult()) {
                    continue;
                }
                Object original = originalFor(spec.behavior());
                Object decided = HookDecision.result(config, spec, original);
                if (spec.policy() == disabled) {
                    assertEquals(spec.id(), original, decided);
                } else {
                    assertNotEquals(spec.id(), original, decided);
                }
            }
        }
    }

    @Test
    public void masterOffPassesEveryOriginalResultThrough() {
        BypassConfig config = BypassConfig.defaults().withMaster(false);
        for (HookSpec spec : ExactHookCatalog.aicrSpecs()) {
            if (!spec.behavior().changesResult()) {
                continue;
            }
            Object original = originalFor(spec.behavior());
            assertEquals(spec.id(), original, HookDecision.result(config, spec, original));
        }
    }

    @Test
    public void argumentHooksArePolicyGated() {
        HookSpec duration = ExactHookCatalog.aicrSpecs().stream()
                .filter(spec -> spec.policy() == Policy.DURATION)
                .findFirst()
                .orElseThrow();
        long original = 100L;
        long now = 500L;

        assertEquals(now, HookDecision.longArgument(BypassConfig.defaults(), duration, original, now));
        assertEquals(
                original,
                HookDecision.longArgument(
                        BypassConfig.defaults().withPolicy(Policy.DURATION, false),
                        duration,
                        original,
                        now
                )
        );
    }

    @Test
    public void everySpecHasAStableUniqueId() {
        List<HookSpec> specs = ExactHookCatalog.aicrSpecs();
        assertEquals(specs.size(), specs.stream().map(HookSpec::id).distinct().count());
        assertTrue(specs.stream().anyMatch(spec -> spec.policy() == Policy.TASK_CONSTRAINTS));
    }

    private static Object originalFor(HookBehavior behavior) {
        return switch (behavior) {
            case RESULT_TRUE -> Boolean.FALSE;
            case RESULT_FALSE -> Boolean.TRUE;
            case RESULT_ZERO_INT -> 77;
            case RESULT_ONE_INT -> 0;
            case RESULT_HUNDRED_INT -> 1;
            case RESULT_ZERO_LONG -> 77L;
            case ARGUMENT_ZERO_BOOLEAN, ARGUMENT_NOW_LONG -> throw new IllegalArgumentException();
        };
    }
}

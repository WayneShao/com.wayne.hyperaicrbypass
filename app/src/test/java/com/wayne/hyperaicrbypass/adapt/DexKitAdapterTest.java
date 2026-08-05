package com.wayne.hyperaicrbypass.adapt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wayne.hyperaicrbypass.config.Policy;
import com.wayne.hyperaicrbypass.hook.HookBehavior;

import org.junit.Test;

import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class DexKitAdapterTest {
    @Test
    public void methodViewMapsToCompleteSemanticTarget() {
        DexKitMethodView view = new DexKitMethodView(
                "com.xiaomi.aicr.next.Status",
                "temperatureGate",
                "boolean",
                List.of("int"),
                Modifier.PUBLIC | Modifier.STATIC,
                Set.of("temperatureLimit:", "temperature:")
        );

        SemanticTarget target = DexKitAdapter.toTarget(view);

        assertEquals(view.className(), target.className());
        assertEquals(view.methodName(), target.methodName());
        assertEquals(view.parameterTypes(), target.parameterTypes());
        assertTrue(target.isStatic());
        assertEquals(view.anchors(), target.anchors());
    }

    @Test
    public void catalogCoversAllAppPoliciesWithSafeLeafBehaviors() {
        EnumSet<Policy> covered = EnumSet.noneOf(Policy.class);
        for (SemanticHookSpec spec : SemanticHookCatalog.specs()) {
            covered.add(spec.policy());
            assertFalse(spec.requiredAnchors().isEmpty());
            assertFalse(Set.of("checkCanStart", "checkCanStop", "getNeedStop", "setRunningStatus")
                    .contains(spec.preferredMethodName()));
            assertTrue(spec.behavior() == HookBehavior.RESULT_TRUE
                    || spec.behavior() == HookBehavior.RESULT_FALSE
                    || spec.behavior() == HookBehavior.RESULT_ZERO_INT
                    || spec.behavior() == HookBehavior.RESULT_ONE_INT
                    || spec.behavior() == HookBehavior.RESULT_HUNDRED_INT
                    || spec.behavior() == HookBehavior.RESULT_ZERO_LONG
                    || spec.behavior() == HookBehavior.ARGUMENT_NOW_LONG);
        }

        assertTrue(covered.containsAll(EnumSet.complementOf(
                EnumSet.of(Policy.TASK_CONSTRAINTS))));
    }
}

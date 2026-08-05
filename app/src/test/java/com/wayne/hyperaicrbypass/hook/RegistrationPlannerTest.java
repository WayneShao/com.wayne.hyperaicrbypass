package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wayne.hyperaicrbypass.config.Policy;

import org.junit.Test;

import java.util.List;

public class RegistrationPlannerTest {
    @Test
    public void registrationBecomesCoveredOnlyAfterSuccessfulHookAndNeverDuplicates() {
        RegistrationPlanner planner = new RegistrationPlanner();
        HookSpec temperature = ExactHookCatalog.aicrSpecs().stream()
                .filter(spec -> spec.policy() == Policy.TEMPERATURE)
                .findFirst()
                .orElseThrow();

        assertTrue(planner.shouldAttempt(temperature));
        assertFalse(planner.isRegistered(temperature));
        planner.recordFailure(temperature, "missing");
        assertTrue(planner.shouldAttempt(temperature));
        assertFalse(planner.isRegistered(temperature));

        planner.recordSuccess(temperature);
        assertTrue(planner.isRegistered(temperature));
        assertFalse(planner.shouldAttempt(temperature));
        assertEquals(1, planner.registeredCount());
        planner.recordSuccess(temperature);
        assertEquals(1, planner.registeredCount());
    }

    @Test
    public void bootstrapGateAcceptsOnlyApprovedTargetProcessOnce() {
        ProcessBootstrapGate gate = new ProcessBootstrapGate(List.of(
                "com.xiaomi.aicr", "com.xiaomi.aiservice"
        ));

        assertFalse(gate.tryAcquire("com.miui.gallery", "com.miui.gallery"));
        assertTrue(gate.tryAcquire("com.xiaomi.aicr", "com.xiaomi.aicr"));
        assertFalse(gate.tryAcquire("com.xiaomi.aicr", "com.xiaomi.aicr"));
        assertFalse(gate.tryAcquire("com.xiaomi.aicr", "com.xiaomi.aicr:remote"));
    }
}

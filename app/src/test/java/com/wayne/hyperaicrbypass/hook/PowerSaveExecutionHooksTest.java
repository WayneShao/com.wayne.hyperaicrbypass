package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.Set;

public class PowerSaveExecutionHooksTest {
    @Test
    public void dedicatedCatalogContainsOnlyConfirmedExecutionBoundaries() {
        List<PowerSaveHookSpec> specs = PowerSaveHookSpec.catalog();

        assertEquals(2, specs.size());
        assertEquals(Set.of("checkCanStart", "getNeedStop"),
                specs.stream().map(PowerSaveHookSpec::methodName)
                        .collect(java.util.stream.Collectors.toSet()));

        PowerSaveHookSpec start = spec("checkCanStart");
        assertEquals("boolean", start.returnType());
        assertEquals(List.of("int"), start.parameterTypes());
        assertEquals(Set.of("checkCanStart error:", "no cloud start config"),
                start.requiredAnchors());
        assertFalse(start.pauseResult());

        PowerSaveHookSpec stop = spec("getNeedStop");
        assertEquals("boolean", stop.returnType());
        assertEquals(List.of(), stop.parameterTypes());
        assertEquals(Set.of("getNeedStop canStop:",
                "running status -> RUNNING_LEVEL_STOP(0)"), stop.requiredAnchors());
        assertTrue(stop.pauseResult());
    }

    @Test
    public void pauseForcesBothBoundariesWhileOtherModesPreserveOriginalResults() {
        PowerSaveHookSpec start = spec("checkCanStart");
        PowerSaveHookSpec stop = spec("getNeedStop");

        assertFalse(PowerSaveExecutionHooks.decide(true, start, true));
        assertTrue(PowerSaveExecutionHooks.decide(true, stop, false));
        assertTrue(PowerSaveExecutionHooks.decide(false, start, true));
        assertFalse(PowerSaveExecutionHooks.decide(false, stop, false));
    }

    @Test
    public void forbiddenCompositeMethodsNeverLeakIntoBypassCatalogs() {
        Set<String> forbidden = Set.of(
                "checkCanStart", "checkCanStop", "getNeedStop", "setRunningStatus"
        );
        assertTrue(ExactHookCatalog.aicrSpecs().stream()
                .noneMatch(spec -> forbidden.contains(spec.methodName())));
        assertTrue(com.wayne.hyperaicrbypass.adapt.SemanticHookCatalog.specs().stream()
                .noneMatch(spec -> forbidden.contains(spec.preferredMethodName())));
    }

    @Test
    public void semanticStaticShapeMustMatchBranchSpecExactly() {
        PowerSaveHookSpec v3Static = PowerSaveHookSpec.catalog(AicrVersionBranch.V3).get(0);
        PowerSaveHookSpec v4Instance = PowerSaveHookSpec.catalog(AicrVersionBranch.V4).get(0);

        assertTrue(PowerSaveExecutionHooks.matchesStaticShape(v3Static, true));
        assertFalse(PowerSaveExecutionHooks.matchesStaticShape(v3Static, false));
        assertTrue(PowerSaveExecutionHooks.matchesStaticShape(v4Instance, false));
        assertFalse(PowerSaveExecutionHooks.matchesStaticShape(v4Instance, true));
    }

    @Test
    public void compactV4UsesStaticStartAndInstanceStopBoundaries() {
        List<PowerSaveHookSpec> specs =
                PowerSaveHookSpec.catalog(AicrRuntimeLayout.V4_COMPACT);

        assertEquals(List.of("d", "r"),
                specs.stream().map(PowerSaveHookSpec::methodName).toList());
        assertTrue(specs.get(0).allowStatic());
        assertFalse(specs.get(1).allowStatic());
        assertEquals("qz7", specs.get(0).className());
        assertEquals("qz7", specs.get(1).className());
    }

    private static PowerSaveHookSpec spec(String methodName) {
        return PowerSaveHookSpec.catalog().stream()
                .filter(spec -> spec.methodName().equals(methodName))
                .findFirst()
                .orElseThrow();
    }
}

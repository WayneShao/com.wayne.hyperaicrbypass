package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.wayne.hyperaicrbypass.config.Policy;

import org.junit.Test;

import java.util.EnumSet;
import java.util.List;

public final class AicrVersionCatalogTest {
    private static final long AICR_3_63 = 2_030_036_300L;
    private static final long AICR_4_0_6 = 2_030_040_006L;

    @Test
    public void version363UsesConfirmedObfuscatedLeafMethods() {
        List<HookSpec> specs = ExactHookCatalog.aicrSpecs(AicrVersionBranch.V3);

        assertTrue(specs.stream().anyMatch(spec ->
                spec.className().equals("u16")
                        && spec.methodName().equals("C")
                        && spec.policy() == Policy.TEMPERATURE));
        assertTrue(specs.stream().anyMatch(spec ->
                spec.className().equals("nt6")
                        && spec.methodName().equals("b")
                        && spec.parameterTypes().equals(List.of("long"))));
        assertTrue(specs.stream().anyMatch(spec ->
                spec.className().equals("c15")
                        && spec.methodName().equals("a")
                        && spec.parameterTypes().equals(List.of("android.content.Context"))));
        assertEquals(EnumSet.allOf(Policy.class), policies(specs));
    }

    @Test
    public void version406KeepsReadableExactFastPath() {
        List<HookSpec> specs = ExactHookCatalog.aicrSpecs(AicrVersionBranch.V4);

        assertTrue(specs.stream().anyMatch(spec ->
                spec.className().equals("com.xiaomi.aicr.searchpro.monitor.RunningStatus")
                        && spec.methodName().equals("getPower")));
        assertTrue(specs.stream().noneMatch(spec -> spec.className().equals("u16")));
        assertEquals(EnumSet.allOf(Policy.class), policies(specs));
    }

    @Test
    public void powerSaveCatalogSelectsBothVersionSpecificBoundaries() {
        List<PowerSaveHookSpec> legacy = PowerSaveHookSpec.catalog(AicrVersionBranch.V3);
        List<PowerSaveHookSpec> modern = PowerSaveHookSpec.catalog(AicrVersionBranch.V4);

        assertEquals(List.of("d", "q"),
                legacy.stream().map(PowerSaveHookSpec::methodName).toList());
        assertEquals(List.of("checkCanStart", "getNeedStop"),
                modern.stream().map(PowerSaveHookSpec::methodName).toList());
        assertTrue(legacy.get(0).allowStatic());
        assertTrue(!legacy.get(1).allowStatic());
    }

    @Test
    public void branchSelectionUsesMajorVersionInsteadOfExactBuild() {
        assertEquals(AicrVersionBranch.V3,
                AicrVersionBranch.detect("3.63.0", AICR_3_63));
        assertEquals(AicrVersionBranch.V3,
                AicrVersionBranch.detect("3.99.8", 2_030_039_908L));
        assertEquals(AicrVersionBranch.V4,
                AicrVersionBranch.detect("4.1.2", 2_030_041_002L));
        assertEquals(AicrVersionBranch.V3,
                AicrVersionBranch.detect(null, 2_030_036_999L));
        assertEquals(AicrVersionBranch.V4,
                AicrVersionBranch.detect("broken", 2_030_040_999L));
        assertEquals(AicrVersionBranch.UNKNOWN,
                AicrVersionBranch.detect("5.0.0", 2_030_050_000L));
    }

    @Test
    public void unknownMajorUsesOnlyStableFrameworkFastPaths() {
        List<HookSpec> specs = ExactHookCatalog.aicrSpecs(AicrVersionBranch.UNKNOWN);

        assertEquals(3, specs.size());
        assertTrue(specs.stream().allMatch(spec ->
                spec.policy() == Policy.TASK_CONSTRAINTS));

        List<PowerSaveHookSpec> powerSave =
                PowerSaveHookSpec.catalog(AicrVersionBranch.UNKNOWN);
        assertEquals(4, powerSave.size());
        assertTrue(powerSave.stream().anyMatch(PowerSaveHookSpec::allowStatic));
        assertTrue(powerSave.stream().anyMatch(spec -> !spec.allowStatic()));
    }

    @Test
    public void semanticOwnersStayInsideTheSelectedAicrBranch() {
        assertTrue(SemanticHooks.isExpectedOwner(AicrVersionBranch.V3, "u16"));
        assertTrue(!SemanticHooks.isExpectedOwner(
                AicrVersionBranch.V3, "com.vendor.analytics.Helper"));
        assertTrue(SemanticHooks.isExpectedOwner(
                AicrVersionBranch.V4,
                "com.xiaomi.aicr.searchpro.monitor.RunningStatus"));
        assertTrue(!SemanticHooks.isExpectedOwner(
                AicrVersionBranch.V4, "com.vendor.RunningStatus"));
    }

    private static EnumSet<Policy> policies(List<HookSpec> specs) {
        EnumSet<Policy> result = EnumSet.noneOf(Policy.class);
        specs.forEach(spec -> result.add(spec.policy()));
        return result;
    }
}

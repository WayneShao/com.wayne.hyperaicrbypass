package com.wayne.hyperaicrbypass.adapt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.wayne.hyperaicrbypass.config.BypassConfig;
import com.wayne.hyperaicrbypass.config.Policy;

import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdaptationModelTest {
    @Test
    public void configRevisionAndRescanGenerationAdvanceIndependently() {
        BypassConfig initial = BypassConfig.defaults();
        BypassConfig mutated = initial.withPolicy(Policy.TEMPERATURE, false);
        BypassConfig rescanned = mutated.nextRescanGeneration();

        assertEquals(1, mutated.getConfigRevision());
        assertEquals(0, mutated.getRescanGeneration());
        assertEquals(mutated.getConfigRevision(), rescanned.getConfigRevision());
        assertEquals(1, rescanned.getRescanGeneration());
    }

    @Test
    public void discoveryKeyExcludesConfigRevision() {
        DiscoveryKey first = new DiscoveryKey(2030040006L, 1234L, 2, 7L);
        DiscoveryKey same = new DiscoveryKey(2030040006L, 1234L, 2, 7L);
        DiscoveryKey rescanned = new DiscoveryKey(2030040006L, 1234L, 2, 8L);

        assertEquals(first, same);
        assertFalse(first.equals(rescanned));
        assertTrue(first.stableValue().contains("2030040006"));
    }

    @Test
    public void semanticCandidateMustMatchEveryShapeAndAnchorAndBeUnique() {
        SemanticQuerySpec spec = new SemanticQuerySpec(
                Policy.TEMPERATURE,
                "com.xiaomi.aicr.",
                "boolean",
                List.of("int"),
                false,
                Set.of("temperatureLimit:", "temperature:")
        );
        SemanticTarget valid = target("RunningStatus", "a", "boolean", List.of("int"), false,
                Set.of("temperatureLimit:", "temperature:"));

        assertEquals(valid, CandidateValidator.selectUnique(spec, List.of(valid), new HashSet<>()));
        assertNull(CandidateValidator.selectUnique(spec, List.of(), new HashSet<>()));
        assertNull(CandidateValidator.selectUnique(spec, List.of(valid, valid.withMethodName("b")),
                new HashSet<>()));
        assertNull(CandidateValidator.selectUnique(spec,
                List.of(valid.withReturnType("int")), new HashSet<>()));
        assertNull(CandidateValidator.selectUnique(spec,
                List.of(valid.withAnchors(Set.of("temperatureLimit:"))), new HashSet<>()));
    }

    @Test
    public void descriptorCannotBeClaimedByTwoPolicies() {
        SemanticQuerySpec spec = new SemanticQuerySpec(
                Policy.OVERLOAD, "com.xiaomi.aicr.", "boolean",
                List.of("android.content.Context", "int"), true, Set.of("is overloadScene:")
        );
        SemanticTarget target = target("OverloadSceneUtil", "a", "boolean",
                spec.parameterTypes(), true, spec.requiredAnchors());
        Set<String> claimed = new HashSet<>();
        claimed.add(target.descriptor());

        assertNull(CandidateValidator.selectUnique(spec, List.of(target), claimed));
    }

    @Test
    public void coverageCountsOnlySuccessfulCurrentKeyRegistrations() {
        DiscoveryKey current = new DiscoveryKey(10, 20, 2, 3);
        DiscoveryKey stale = new DiscoveryKey(9, 20, 2, 3);
        List<CoverageReport> reports = new ArrayList<>();
        reports.add(CoverageReport.success(
                Policy.TEMPERATURE, CoverageLayer.EXACT, "A#a()", "com.xiaomi.aicr", current, 4
        ));
        reports.add(CoverageReport.success(
                Policy.MIGRATION, CoverageLayer.PARTIAL, "B#b()", "com.xiaomi.aicr", current, 4
        ));
        reports.add(CoverageReport.failure(
                Policy.POWER, CoverageLayer.UNAVAILABLE, "com.xiaomi.aicr", current, 4, "not found"
        ));
        reports.add(CoverageReport.success(
                Policy.CHARGING, CoverageLayer.EXACT, "Old#a()", "com.xiaomi.aicr", stale, 4
        ));

        Map<Policy, CoverageReport> aggregate = CoverageAggregator.forKey(current, reports);

        assertEquals(CoverageLayer.EXACT, aggregate.get(Policy.TEMPERATURE).layer());
        assertEquals(CoverageLayer.UNAVAILABLE, aggregate.get(Policy.POWER).layer());
        assertEquals(CoverageLayer.PENDING, aggregate.get(Policy.CHARGING).layer());
        assertEquals(2, CoverageAggregator.successCount(aggregate));
    }

    @Test(expected = IllegalArgumentException.class)
    public void matchedButUnregisteredDescriptorCannotClaimSuccess() {
        new CoverageReport(
                Policy.TEMPERATURE,
                CoverageLayer.EXACT,
                "A#a()",
                "com.xiaomi.aicr",
                new DiscoveryKey(1, 2, 2, 0),
                0,
                false,
                ""
        );
    }

    private static SemanticTarget target(
            String simpleClass,
            String method,
            String returnType,
            List<String> parameters,
            boolean isStatic,
            Set<String> anchors
    ) {
        return new SemanticTarget(
                "com.xiaomi.aicr." + simpleClass,
                method,
                returnType,
                parameters,
                isStatic,
                anchors
        );
    }
}

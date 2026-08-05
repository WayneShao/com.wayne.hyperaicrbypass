package com.wayne.hyperaicrbypass.adapt;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CandidateValidator {
    private CandidateValidator() {
    }

    public static SemanticTarget selectUnique(
            SemanticQuerySpec spec,
            List<SemanticTarget> candidates,
            Set<String> claimedDescriptors
    ) {
        List<SemanticTarget> matches = candidates.stream()
                .filter(candidate -> matches(spec, candidate))
                .filter(candidate -> !claimedDescriptors.contains(candidate.descriptor()))
                .distinct()
                .collect(Collectors.toList());
        return matches.size() == 1 ? matches.get(0) : null;
    }

    public static boolean matches(SemanticQuerySpec spec, SemanticTarget candidate) {
        return candidate.className().startsWith(spec.packagePrefix())
                && candidate.returnType().equals(spec.returnType())
                && candidate.parameterTypes().equals(spec.parameterTypes())
                && candidate.isStatic() == spec.isStatic()
                && candidate.anchors().containsAll(spec.requiredAnchors());
    }
}

package com.wayne.hyperaicrbypass.adapt;

import com.wayne.hyperaicrbypass.config.Policy;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record SemanticQuerySpec(
        Policy policy,
        String packagePrefix,
        String returnType,
        List<String> parameterTypes,
        boolean isStatic,
        Set<String> requiredAnchors
) {
    public SemanticQuerySpec {
        Objects.requireNonNull(policy);
        Objects.requireNonNull(packagePrefix);
        Objects.requireNonNull(returnType);
        parameterTypes = List.copyOf(parameterTypes);
        requiredAnchors = Set.copyOf(requiredAnchors);
        if (packagePrefix.isBlank() || requiredAnchors.isEmpty()) {
            throw new IllegalArgumentException("Semantic queries require a package and anchors");
        }
    }
}

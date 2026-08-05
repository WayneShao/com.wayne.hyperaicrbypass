package com.wayne.hyperaicrbypass.adapt;

import com.wayne.hyperaicrbypass.config.Policy;
import com.wayne.hyperaicrbypass.hook.HookBehavior;

import java.util.List;
import java.util.Set;

public record SemanticHookSpec(
        Policy policy,
        String preferredMethodName,
        String returnType,
        List<String> parameterTypes,
        boolean isStatic,
        Set<String> requiredAnchors,
        HookBehavior behavior
) {
    public SemanticHookSpec {
        parameterTypes = List.copyOf(parameterTypes);
        requiredAnchors = Set.copyOf(requiredAnchors);
        if (requiredAnchors.isEmpty()) {
            throw new IllegalArgumentException("Semantic hooks require at least one anchor");
        }
    }
}

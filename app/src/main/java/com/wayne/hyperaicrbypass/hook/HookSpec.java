package com.wayne.hyperaicrbypass.hook;

import com.wayne.hyperaicrbypass.config.Policy;

import java.util.List;
import java.util.Objects;

public record HookSpec(
        String className,
        String methodName,
        String returnType,
        List<String> parameterTypes,
        Policy policy,
        HookBehavior behavior
) {
    public HookSpec {
        Objects.requireNonNull(className);
        Objects.requireNonNull(methodName);
        Objects.requireNonNull(returnType);
        parameterTypes = List.copyOf(parameterTypes);
        Objects.requireNonNull(policy);
        Objects.requireNonNull(behavior);
    }

    public String id() {
        return className + "#" + methodName + "(" + String.join(",", parameterTypes) + ")";
    }

    public int parameterCount() {
        return parameterTypes.size();
    }
}

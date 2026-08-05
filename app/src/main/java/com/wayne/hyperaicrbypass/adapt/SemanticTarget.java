package com.wayne.hyperaicrbypass.adapt;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record SemanticTarget(
        String className,
        String methodName,
        String returnType,
        List<String> parameterTypes,
        boolean isStatic,
        Set<String> anchors
) {
    public SemanticTarget {
        Objects.requireNonNull(className);
        Objects.requireNonNull(methodName);
        Objects.requireNonNull(returnType);
        parameterTypes = List.copyOf(parameterTypes);
        anchors = Set.copyOf(anchors);
    }

    public String descriptor() {
        return className + "#" + methodName + "(" + String.join(",", parameterTypes) + ")";
    }

    public SemanticTarget withMethodName(String value) {
        return new SemanticTarget(className, value, returnType, parameterTypes, isStatic, anchors);
    }

    public SemanticTarget withReturnType(String value) {
        return new SemanticTarget(className, methodName, value, parameterTypes, isStatic, anchors);
    }

    public SemanticTarget withAnchors(Set<String> value) {
        return new SemanticTarget(className, methodName, returnType, parameterTypes, isStatic, value);
    }
}

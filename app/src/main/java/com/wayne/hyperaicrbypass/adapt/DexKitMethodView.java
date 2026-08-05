package com.wayne.hyperaicrbypass.adapt;

import java.util.List;
import java.util.Set;

public record DexKitMethodView(
        String className,
        String methodName,
        String returnType,
        List<String> parameterTypes,
        int modifiers,
        Set<String> anchors
) {
    public DexKitMethodView {
        parameterTypes = List.copyOf(parameterTypes);
        anchors = Set.copyOf(anchors);
    }
}

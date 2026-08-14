package com.wayne.hyperaicrbypass.hook;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import kotlin.jvm.functions.Function3;

final class SemanticMethodShape {
    private SemanticMethodShape() {
    }

    static boolean matches(
            Method method,
            String returnType,
            List<String> parameterTypes,
            boolean isStatic,
            boolean allowFunction3Implementation
    ) {
        if (!method.getReturnType().getName().equals(returnType)
                || Modifier.isStatic(method.getModifiers()) != isStatic) {
            return false;
        }
        Class<?>[] actualTypes = method.getParameterTypes();
        if (actualTypes.length != parameterTypes.size()) {
            return false;
        }
        for (int index = 0; index < actualTypes.length; index++) {
            if (actualTypes[index].getName().equals(parameterTypes.get(index))) {
                continue;
            }
            if (!allowFunction3Implementation
                    || index != actualTypes.length - 1
                    || !Function3.class.isAssignableFrom(actualTypes[index])) {
                return false;
            }
        }
        return true;
    }
}

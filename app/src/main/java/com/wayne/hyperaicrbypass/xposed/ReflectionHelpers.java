package com.wayne.hyperaicrbypass.xposed;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.libxposed.api.XposedInterface;

public final class ReflectionHelpers {
    private ReflectionHelpers() {
    }

    public static Class<?> findClass(String name, ClassLoader classLoader) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException error) {
            throw new IllegalArgumentException("Class not found: " + name, error);
        }
    }

    public static XposedInterface.HookHandle findAndHookMethod(
            String className,
            ClassLoader classLoader,
            String methodName,
            Object... parameterTypesAndCallback
    ) {
        return findAndHookMethod(
                findClass(className, classLoader), methodName, parameterTypesAndCallback);
    }

    public static XposedInterface.HookHandle findAndHookMethod(
            Class<?> owner,
            String methodName,
            Object... parameterTypesAndCallback
    ) {
        if (parameterTypesAndCallback.length == 0
                || !(parameterTypesAndCallback[parameterTypesAndCallback.length - 1]
                instanceof ModernHook callback)) {
            throw new IllegalArgumentException("Last argument must be a ModernHook callback");
        }
        Class<?>[] parameterTypes = new Class<?>[parameterTypesAndCallback.length - 1];
        for (int index = 0; index < parameterTypes.length; index++) {
            Object type = parameterTypesAndCallback[index];
            if (type instanceof Class<?> clazz) {
                parameterTypes[index] = clazz;
            } else if (type instanceof String name) {
                parameterTypes[index] = findClass(name, owner.getClassLoader());
            } else {
                throw new IllegalArgumentException("Unsupported parameter type " + type);
            }
        }
        Method method = findMethod(owner, methodName, parameterTypes);
        return ModernXposed.hookMethod(method, callback);
    }

    public static Object getObjectField(Object instance, String name) {
        if (instance == null) {
            throw new NullPointerException("Cannot read field " + name + " from null");
        }
        return readField(findField(instance.getClass(), name), instance);
    }

    public static Object getStaticObjectField(Class<?> owner, String name) {
        Field field = findField(owner, name);
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException(name + " is not static on " + owner.getName());
        }
        return readField(field, null);
    }

    public static Object callMethod(Object instance, String name, Object... args) {
        if (instance == null) {
            throw new NullPointerException("Cannot call " + name + " on null");
        }
        Method method = findCompatibleMethod(instance.getClass(), name, args);
        try {
            return method.invoke(instance, args);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Cannot call " + method, error);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error fatal) {
                throw fatal;
            }
            throw new IllegalStateException("Call failed for " + method, cause);
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>[] parameterTypes) {
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new IllegalArgumentException("Method not found: " + owner.getName() + '#' + name);
    }

    private static Method findCompatibleMethod(Class<?> owner, String name, Object[] args) {
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (!method.getName().equals(name) || parameterTypes.length != args.length) {
                    continue;
                }
                boolean compatible = true;
                for (int index = 0; index < args.length; index++) {
                    if (!accepts(parameterTypes[index], args[index])) {
                        compatible = false;
                        break;
                    }
                }
                if (compatible) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        throw new IllegalArgumentException("Compatible method not found: "
                + owner.getName() + '#' + name);
    }

    private static boolean accepts(Class<?> parameterType, Object argument) {
        if (argument == null) {
            return !parameterType.isPrimitive();
        }
        if (!parameterType.isPrimitive()) {
            return parameterType.isInstance(argument);
        }
        return (parameterType == boolean.class && argument instanceof Boolean)
                || (parameterType == byte.class && argument instanceof Byte)
                || (parameterType == char.class && argument instanceof Character)
                || (parameterType == short.class && argument instanceof Short)
                || (parameterType == int.class && argument instanceof Integer)
                || (parameterType == long.class && argument instanceof Long)
                || (parameterType == float.class && argument instanceof Float)
                || (parameterType == double.class && argument instanceof Double);
    }

    private static Field findField(Class<?> owner, String name) {
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new IllegalArgumentException("Field not found: " + owner.getName() + '#' + name);
    }

    private static Object readField(Field field, Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Cannot read " + field, error);
        }
    }
}

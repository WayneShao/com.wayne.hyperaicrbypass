package com.wayne.hyperaicrbypass.xposed;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ReflectionHelpersTest {
    @Test
    public void readsInheritedPrivateField() {
        assertEquals("value", ReflectionHelpers.getObjectField(new Child(), "hidden"));
    }

    @Test
    public void callsCompatiblePrivateMethod() {
        assertEquals(8, ReflectionHelpers.callMethod(new Child(), "increment", 7));
    }

    private static class Parent {
        private final String hidden = "value";

        private int increment(int value) {
            return value + 1;
        }
    }

    private static final class Child extends Parent {
    }
}

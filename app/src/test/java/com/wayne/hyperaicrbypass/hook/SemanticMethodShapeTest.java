package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import kotlin.jvm.functions.Function3;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

public final class SemanticMethodShapeTest {
    @Test
    public void acceptsObfuscatedFunction3ImplementationForCompactTransport() throws Exception {
        Method method = Fixture.class.getDeclaredMethod(
                "transport", int.class, boolean.class, ObfuscatedCallback.class);

        assertTrue(SemanticMethodShape.matches(
                method,
                "android.os.Bundle",
                List.of("int", "boolean", "rb8"),
                false,
                true
        ));
    }

    @Test
    public void rejectsUnrelatedThirdParameterAndWrongStaticShape() throws Exception {
        Method unrelated = Fixture.class.getDeclaredMethod(
                "unrelated", int.class, boolean.class, Object.class);
        Method staticTransport = Fixture.class.getDeclaredMethod(
                "staticTransport", int.class, boolean.class, ObfuscatedCallback.class);

        assertFalse(SemanticMethodShape.matches(
                unrelated,
                "android.os.Bundle",
                List.of("int", "boolean", "rb8"),
                false,
                true
        ));
        assertFalse(SemanticMethodShape.matches(
                staticTransport,
                "android.os.Bundle",
                List.of("int", "boolean", "rb8"),
                false,
                true
        ));
    }

    private static final class Fixture {
        android.os.Bundle transport(int scope, boolean cache, ObfuscatedCallback callback) {
            return null;
        }

        android.os.Bundle unrelated(int scope, boolean cache, Object callback) {
            return null;
        }

        static android.os.Bundle staticTransport(
                int scope,
                boolean cache,
                ObfuscatedCallback callback
        ) {
            return null;
        }
    }

    private static final class ObfuscatedCallback
            implements Function3<Object, Object, Object, Object> {
        @Override
        public Object invoke(Object first, Object second, Object third) {
            return null;
        }
    }
}

package com.wayne.hyperaicrbypass.xposed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ModernHookParamTest {
    @Test
    public void setResultSkipsOriginalAndClearsThrowable() {
        ModernHook.MethodHookParam param =
                new ModernHook.MethodHookParam(new Object(), new Object[]{1});
        param.throwable(new IllegalStateException("original failed"));

        param.setResult(2);

        assertTrue(param.returnEarly());
        assertNull(param.throwable());
        assertEquals(2, param.getResult());
    }

    @Test
    public void objectExtrasAreLocalToInvocation() {
        ModernHook.MethodHookParam first =
                new ModernHook.MethodHookParam(null, new Object[0]);
        ModernHook.MethodHookParam second =
                new ModernHook.MethodHookParam(null, new Object[0]);

        first.setObjectExtra("token", 7);

        assertEquals(7, first.getObjectExtra("token"));
        assertNull(second.getObjectExtra("token"));
        first.setObjectExtra("token", null);
        assertNull(first.getObjectExtra("token"));
        assertFalse(second.returnEarly());
    }
}

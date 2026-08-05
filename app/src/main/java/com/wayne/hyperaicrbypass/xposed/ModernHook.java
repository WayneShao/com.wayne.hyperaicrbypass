package com.wayne.hyperaicrbypass.xposed;

import java.util.HashMap;
import java.util.Map;

public abstract class ModernHook {
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
    }

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    public static final class MethodHookParam {
        public final Object thisObject;
        public final Object[] args;
        private final Map<String, Object> extras = new HashMap<>();
        private Object result;
        private Throwable throwable;
        private boolean returnEarly;

        MethodHookParam(Object thisObject, Object[] args) {
            this.thisObject = thisObject;
            this.args = args;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
            throwable = null;
            returnEarly = true;
        }

        public Object getObjectExtra(String key) {
            return extras.get(key);
        }

        public void setObjectExtra(String key, Object value) {
            if (value == null) {
                extras.remove(key);
            } else {
                extras.put(key, value);
            }
        }

        Object result() {
            return result;
        }

        void result(Object result) {
            this.result = result;
        }

        Throwable throwable() {
            return throwable;
        }

        void throwable(Throwable throwable) {
            this.throwable = throwable;
        }

        boolean returnEarly() {
            return returnEarly;
        }
    }
}

package com.wayne.hyperaicrbypass.adapt;

import java.lang.reflect.Modifier;

public final class DexKitAdapter {
    private DexKitAdapter() {
    }

    public static SemanticTarget toTarget(DexKitMethodView view) {
        return new SemanticTarget(
                view.className(),
                view.methodName(),
                view.returnType(),
                view.parameterTypes(),
                Modifier.isStatic(view.modifiers()),
                view.anchors()
        );
    }
}

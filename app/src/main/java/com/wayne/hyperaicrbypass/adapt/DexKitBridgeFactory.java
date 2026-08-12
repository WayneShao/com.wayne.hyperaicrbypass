package com.wayne.hyperaicrbypass.adapt;

import android.content.Context;

import org.luckypray.dexkit.DexKitBridge;

public final class DexKitBridgeFactory {
    private DexKitBridgeFactory() {
    }

    public static DexKitBridge create(Context targetContext) {
        DexKitNativeLoader.ensureLoaded(targetContext);
        return DexKitBridge.create(targetContext.getApplicationInfo().sourceDir);
    }
}

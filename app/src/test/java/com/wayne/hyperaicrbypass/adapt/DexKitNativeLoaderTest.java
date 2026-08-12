package com.wayne.hyperaicrbypass.adapt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.List;
import java.util.Set;

public final class DexKitNativeLoaderTest {
    @Test
    public void selectsFirstSupportedAbiPresentInModuleApk() {
        String abi = DexKitNativeLoader.selectAbi(
                List.of("arm64-v8a", "armeabi-v7a"),
                Set.of("lib/armeabi-v7a/libdexkit.so", "lib/arm64-v8a/libdexkit.so")
        );

        assertEquals("arm64-v8a", abi);
    }

    @Test
    public void rejectsModuleApkWithoutCompatibleDexKitLibrary() {
        assertThrows(IllegalStateException.class, () -> DexKitNativeLoader.selectAbi(
                List.of("arm64-v8a"),
                Set.of("lib/x86_64/libdexkit.so")
        ));
    }

    @Test
    public void buildsAndroidZipLibraryPath() {
        assertEquals(
                "/data/app/module/base.apk!/lib/arm64-v8a/libdexkit.so",
                DexKitNativeLoader.libraryPath(
                        "/data/app/module/base.apk", "arm64-v8a"
                )
        );
    }
}

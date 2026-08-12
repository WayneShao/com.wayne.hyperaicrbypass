package com.wayne.hyperaicrbypass.adapt;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DexKitNativeLoader {
    private static final String MODULE_PACKAGE = "com.wayne.hyperaicrbypass";
    private static final String LIBRARY_NAME = "dexkit";
    private static boolean loaded;

    private DexKitNativeLoader() {
    }

    public static synchronized void ensureLoaded(Context context) {
        if (loaded) {
            return;
        }
        try {
            System.loadLibrary(LIBRARY_NAME);
            loaded = true;
            return;
        } catch (UnsatisfiedLinkError ignored) {
            // The injected module class loader does not always expose APK native libraries.
        }

        try {
            ApplicationInfo module = context.getPackageManager().getApplicationInfo(
                    MODULE_PACKAGE, 0
            );
            Set<String> entries = zipEntries(module.sourceDir);
            String abi = selectAbi(Arrays.asList(Build.SUPPORTED_ABIS), entries);
            System.load(libraryPath(module.sourceDir, abi));
            loaded = true;
        } catch (Throwable error) {
            throw new IllegalStateException("Unable to load DexKit native library", error);
        }
    }

    static String selectAbi(List<String> supportedAbis, Set<String> apkEntries) {
        for (String abi : supportedAbis) {
            if (apkEntries.contains(entryName(abi))) {
                return abi;
            }
        }
        throw new IllegalStateException("No compatible libdexkit.so in module APK");
    }

    static String libraryPath(String apkPath, String abi) {
        return apkPath + "!/" + entryName(abi);
    }

    private static String entryName(String abi) {
        return "lib/" + abi + "/libdexkit.so";
    }

    private static Set<String> zipEntries(String apkPath) throws IOException {
        Set<String> entries = new HashSet<>();
        try (ZipFile apk = new ZipFile(apkPath)) {
            var iterator = apk.entries();
            while (iterator.hasMoreElements()) {
                ZipEntry entry = iterator.nextElement();
                entries.add(entry.getName());
            }
        }
        return entries;
    }
}

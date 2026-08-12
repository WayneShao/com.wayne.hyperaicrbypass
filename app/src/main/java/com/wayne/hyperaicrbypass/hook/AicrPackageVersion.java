package com.wayne.hyperaicrbypass.hook;

import android.content.Context;

final class AicrPackageVersion {
    private AicrPackageVersion() {
    }

    static long read(Context context) {
        return readInfo(context).versionCode();
    }

    static AicrVersionBranch branch(Context context) {
        Info info = readInfo(context);
        return AicrVersionBranch.detect(info.versionName(), info.versionCode());
    }

    private static Info readInfo(Context context) {
        try {
            android.content.pm.PackageInfo info = context.getPackageManager()
                    .getPackageInfo("com.xiaomi.aicr", 0);
            return new Info(info.versionName, info.getLongVersionCode());
        } catch (Exception ignored) {
            return new Info(null, 0L);
        }
    }

    private record Info(String versionName, long versionCode) {
    }
}

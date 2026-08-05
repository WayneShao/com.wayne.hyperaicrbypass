package com.wayne.hyperaicrbypass.config;

import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

public final class CallerAuthorizer {
    public static final String MODULE_PACKAGE = "com.example.hyperaicrbypass";
    public static final String AICR_PACKAGE = "com.xiaomi.aicr";
    public static final String AI_SERVICE_PACKAGE = "com.xiaomi.aiservice";
    public static final String GALLERY_PACKAGE = "com.miui.gallery";

    private final int moduleUid;
    private final IntFunction<List<String>> packagesForUid;

    public CallerAuthorizer(int moduleUid, IntFunction<List<String>> packagesForUid) {
        this.moduleUid = moduleUid;
        this.packagesForUid = Objects.requireNonNull(packagesForUid);
    }

    public boolean canMutate(int uid) {
        return uid == moduleUid;
    }

    public boolean canReadSnapshot(int uid) {
        return canMutate(uid)
                || hasPackage(uid, AICR_PACKAGE)
                || hasPackage(uid, AI_SERVICE_PACKAGE)
                || hasPackage(uid, GALLERY_PACKAGE);
    }

    public boolean canReportCoverage(int uid) {
        return hasPackage(uid, AICR_PACKAGE) || hasPackage(uid, AI_SERVICE_PACKAGE);
    }

    private boolean hasPackage(int uid, String packageName) {
        List<String> packages = packagesForUid.apply(uid);
        return packages != null && packages.contains(packageName);
    }
}

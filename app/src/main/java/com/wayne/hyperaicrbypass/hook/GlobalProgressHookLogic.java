package com.wayne.hyperaicrbypass.hook;

public final class GlobalProgressHookLogic {
    private GlobalProgressHookLogic() {
    }

    public static boolean shouldForceNotification(
            boolean ready,
            int scope,
            GlobalProgressSnapshot snapshot,
            long currentRunStartTime,
            long nowElapsedRealtime
    ) {
        if (!ready || snapshot == null
                || !snapshot.isCompatible(
                        snapshot.fixedProgress(), currentRunStartTime, nowElapsedRealtime)) {
            return false;
        }
        if (snapshot.branch() == GlobalProgressBranch.UNMIGRATED_LOCAL) {
            return scope == 2 || scope == 4 || scope == 8 || scope == 16;
        }
        return scope == 1 || scope == 2 || scope == 4 || scope == 8 || scope == 16;
    }
}

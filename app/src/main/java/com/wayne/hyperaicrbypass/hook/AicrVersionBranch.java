package com.wayne.hyperaicrbypass.hook;

public enum AicrVersionBranch {
    V3,
    V4,
    UNKNOWN;

    public static AicrVersionBranch detect(String versionName, long versionCode) {
        int nameMajor = parseMajor(versionName);
        if (nameMajor == 3) {
            return V3;
        }
        if (nameMajor == 4) {
            return V4;
        }
        if (nameMajor > 0) {
            return UNKNOWN;
        }

        long encodedMajor = (versionCode / 10_000L) % 100L;
        if (encodedMajor == 3) {
            return V3;
        }
        if (encodedMajor == 4) {
            return V4;
        }
        return UNKNOWN;
    }

    private static int parseMajor(String versionName) {
        if (versionName == null) {
            return -1;
        }
        int separator = versionName.indexOf('.');
        String value = separator >= 0 ? versionName.substring(0, separator) : versionName;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}

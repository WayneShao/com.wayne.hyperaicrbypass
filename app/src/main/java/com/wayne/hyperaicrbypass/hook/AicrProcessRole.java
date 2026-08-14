package com.wayne.hyperaicrbypass.hook;

public enum AicrProcessRole {
    MAIN,
    SEARCH_DATA,
    SEARCH_UI,
    SEARCH_SERVICE,
    IGNORE;

    public static AicrProcessRole forProcess(String processName) {
        if ("com.xiaomi.aicr".equals(processName)) {
            return MAIN;
        }
        if ("com.xiaomi.aicr:searchDataService".equals(processName)) {
            return SEARCH_DATA;
        }
        if ("com.xiaomi.aicr:searchDataService_ui".equals(processName)) {
            return SEARCH_UI;
        }
        if ("com.xiaomi.aicr:searchService".equals(processName)) {
            return SEARCH_SERVICE;
        }
        return IGNORE;
    }
}

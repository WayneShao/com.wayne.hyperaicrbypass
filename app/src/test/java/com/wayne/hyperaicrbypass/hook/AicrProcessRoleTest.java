package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class AicrProcessRoleTest {
    @Test
    public void mapsOnlyFunctionalAicrProcessesToHookRoles() {
        assertEquals(AicrProcessRole.MAIN,
                AicrProcessRole.forProcess("com.xiaomi.aicr"));
        assertEquals(AicrProcessRole.SEARCH_DATA,
                AicrProcessRole.forProcess("com.xiaomi.aicr:searchDataService"));
        assertEquals(AicrProcessRole.SEARCH_UI,
                AicrProcessRole.forProcess("com.xiaomi.aicr:searchDataService_ui"));
        assertEquals(AicrProcessRole.SEARCH_SERVICE,
                AicrProcessRole.forProcess("com.xiaomi.aicr:searchService"));
        assertEquals(AicrProcessRole.IGNORE,
                AicrProcessRole.forProcess("com.xiaomi.aicr:cognitionService"));
        assertEquals(AicrProcessRole.IGNORE,
                AicrProcessRole.forProcess("com.xiaomi.aicr:nfcService"));
    }
}

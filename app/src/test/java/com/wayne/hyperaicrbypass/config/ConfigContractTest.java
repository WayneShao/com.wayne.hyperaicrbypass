package com.wayne.hyperaicrbypass.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.Map;

public class ConfigContractTest {
    @Test
    public void stableAuthorityAndOperationsAreFixed() {
        assertEquals("com.example.hyperaicrbypass.settings", ConfigContract.AUTHORITY);
        assertEquals("get_snapshot", ConfigContract.METHOD_GET_SNAPSHOT);
        assertEquals("set_policy", ConfigContract.METHOD_SET_POLICY);
        assertEquals("report_coverage", ConfigContract.METHOD_REPORT_COVERAGE);
    }

    @Test
    public void onlyModuleUidCanMutateOrRescan() {
        CallerAuthorizer authorizer = authorizer();

        assertTrue(authorizer.canMutate(1000));
        assertFalse(authorizer.canMutate(2000));
        assertFalse(authorizer.canMutate(3000));
        assertFalse(authorizer.canMutate(4000));
    }

    @Test
    public void approvedTargetsCanReadButOnlyAicrTargetsCanReport() {
        CallerAuthorizer authorizer = authorizer();

        assertTrue(authorizer.canReadSnapshot(1000));
        assertTrue(authorizer.canReadSnapshot(2000));
        assertTrue(authorizer.canReadSnapshot(3000));
        assertFalse(authorizer.canReadSnapshot(4000));
        assertTrue(authorizer.canReportCoverage(2000));
        assertFalse(authorizer.canReportCoverage(3000));
        assertFalse(authorizer.canReportCoverage(4000));
    }

    @Test
    public void codecRoundTripsImmutableSnapshotValues() {
        BypassConfig source = BypassConfig.defaults()
                .withPolicy(Policy.CHARGING, false)
                .nextRescanGeneration();

        Map<String, Object> encoded = ConfigCodec.encode(source);
        BypassConfig decoded = ConfigCodec.decode(encoded);

        assertEquals(source, decoded);
        assertThrows(UnsupportedOperationException.class,
                () -> encoded.put(ConfigContract.KEY_MASTER, false));
    }

    @Test
    public void codecRejectsUnknownKeysWrongTypesAndOversizedValues() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfigCodec.decode(Map.of("unknown", true)));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigCodec.decode(Map.of(ConfigContract.KEY_MASTER, "yes")));
        assertThrows(IllegalArgumentException.class,
                () -> ConfigContract.requireShortText("x".repeat(513), "descriptor"));
    }

    private static CallerAuthorizer authorizer() {
        return new CallerAuthorizer(1000, uid -> switch (uid) {
            case 1000 -> List.of("com.example.hyperaicrbypass");
            case 2000 -> List.of("com.xiaomi.aicr", "com.xiaomi.aiservice");
            case 3000 -> List.of("com.miui.gallery");
            default -> List.of("other.app");
        });
    }
}

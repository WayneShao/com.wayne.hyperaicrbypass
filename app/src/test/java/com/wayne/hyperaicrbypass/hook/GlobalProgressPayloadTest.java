package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public final class GlobalProgressPayloadTest {
    @Test
    public void roundTripsWithoutOverwritingGalleryPayloadKeys() {
        GlobalProgressSnapshot snapshot = new GlobalProgressSnapshot(
                85_317, 85, GlobalProgressBranch.MIGRATED_DIRECT_AI,
                12_345L, 7L, 8_000L
        );
        Map<String, Object> carrier = new HashMap<>();
        carrier.put(PreciseProgressPayload.KEY_VERSION, 1L);

        carrier.putAll(GlobalProgressPayload.encode(snapshot));

        assertEquals(1L, carrier.get(PreciseProgressPayload.KEY_VERSION));
        assertTrue(GlobalProgressPayload.encode(snapshot).keySet().stream().allMatch(
                key -> key.startsWith(
                        "com.wayne.hyperaicrbypass.global_precise_progress.")));
        assertEquals(snapshot, GlobalProgressPayload.decode(carrier).orElseThrow());
    }
}

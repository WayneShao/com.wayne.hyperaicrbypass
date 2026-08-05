package com.wayne.hyperaicrbypass.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public final class PreciseProgressPayloadTest {
    @Test
    public void roundTripsNamespacedVersionedValues() {
        PreciseProgressSnapshot snapshot = snapshot();

        Map<String, Long> encoded = PreciseProgressPayload.encode(snapshot);

        assertEquals(5, encoded.size());
        assertTrue(encoded.keySet().stream().allMatch(
                key -> key.startsWith("com.wayne.hyperaicrbypass.precise_progress.")));
        assertEquals(snapshot, PreciseProgressPayload.decode(encoded).orElseThrow());
    }

    @Test
    public void rejectsMissingWrongVersionAndWrongTypes() {
        Map<String, Long> valid = PreciseProgressPayload.encode(snapshot());

        Map<String, Object> missing = new HashMap<>(valid);
        missing.remove(PreciseProgressPayload.KEY_NUMERATOR);
        assertFalse(PreciseProgressPayload.decode(missing).isPresent());

        Map<String, Object> wrongVersion = new HashMap<>(valid);
        wrongVersion.put(PreciseProgressPayload.KEY_VERSION, 2L);
        assertFalse(PreciseProgressPayload.decode(wrongVersion).isPresent());

        Map<String, Object> wrongType = new HashMap<>(valid);
        wrongType.put(PreciseProgressPayload.KEY_FIXED_PROGRESS, "70");
        assertFalse(PreciseProgressPayload.decode(wrongType).isPresent());
    }

    @Test
    public void rejectsValuesThatCannotRestoreAValidSnapshot() {
        Map<String, Object> invalid = new HashMap<>(
                PreciseProgressPayload.encode(snapshot())
        );
        invalid.put(PreciseProgressPayload.KEY_DENOMINATOR, -1L);

        assertFalse(PreciseProgressPayload.decode(invalid).isPresent());
    }

    private static PreciseProgressSnapshot snapshot() {
        return PreciseProgressSnapshot.restore(
                418_714L, 595_999L, 70, 1_234L
        ).orElseThrow();
    }
}

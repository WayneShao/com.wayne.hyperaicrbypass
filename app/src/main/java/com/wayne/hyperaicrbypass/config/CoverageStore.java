package com.wayne.hyperaicrbypass.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.wayne.hyperaicrbypass.adapt.CoverageLayer;

import java.util.EnumMap;
import java.util.Map;

final class CoverageStore {
    private static final String PREFERENCES = "coverage_status";

    private final SharedPreferences preferences;

    CoverageStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    synchronized void report(Policy policy, CoverageLayer layer, long generation) {
        String generationKey = ConfigContract.coverageKey(policy) + ".generation";
        if (generation < preferences.getLong(generationKey, -1L)) {
            return;
        }
        if (!preferences.edit()
                .putString(ConfigContract.coverageKey(policy), layer.name())
                .putLong(generationKey, generation)
                .commit()) {
            throw new IllegalStateException("Unable to persist coverage status");
        }
    }

    synchronized Map<Policy, CoverageLayer> read() {
        EnumMap<Policy, CoverageLayer> result = new EnumMap<>(Policy.class);
        for (Policy policy : Policy.values()) {
            String value = preferences.getString(ConfigContract.coverageKey(policy), null);
            if (value == null) {
                continue;
            }
            try {
                result.put(policy, CoverageLayer.valueOf(value));
            } catch (IllegalArgumentException ignored) {
                // Ignore stale values from a future or malformed version.
            }
        }
        return result;
    }
}

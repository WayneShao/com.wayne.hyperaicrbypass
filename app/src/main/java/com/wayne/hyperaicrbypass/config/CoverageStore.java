package com.wayne.hyperaicrbypass.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.wayne.hyperaicrbypass.adapt.CoverageLayer;
import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;
import com.wayne.hyperaicrbypass.hook.ExecutionCoverage;

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

    synchronized void reportExecution(DiscoveryKey key, ExecutionCoverage coverage) {
        String currentKey = preferences.getString(
                ConfigContract.KEY_EXECUTION_DISCOVERY_KEY, null
        );
        if (coverage != ExecutionCoverage.PENDING
                && !key.stableValue().equals(currentKey)) {
            return;
        }
        if (!preferences.edit()
                .putString(ConfigContract.KEY_EXECUTION_DISCOVERY_KEY, key.stableValue())
                .putString(ConfigContract.KEY_EXECUTION_COVERAGE, coverage.name())
                .putLong(ConfigContract.KEY_DISCOVERY_VERSION_CODE, key.versionCode())
                .putLong(ConfigContract.KEY_DISCOVERY_UPDATE_TIME, key.lastUpdateTime())
                .putInt(ConfigContract.KEY_DISCOVERY_SCHEMA_REVISION, key.schemaRevision())
                .putLong(ConfigContract.KEY_RESCAN_GENERATION, key.rescanGeneration())
                .commit()) {
            throw new IllegalStateException("Unable to persist execution coverage");
        }
    }

    synchronized ExecutionRecord readExecution() {
        String coverageValue = preferences.getString(
                ConfigContract.KEY_EXECUTION_COVERAGE, ExecutionCoverage.PENDING.name()
        );
        ExecutionCoverage coverage;
        try {
            coverage = ExecutionCoverage.valueOf(coverageValue);
        } catch (IllegalArgumentException ignored) {
            coverage = ExecutionCoverage.PENDING;
        }
        DiscoveryKey key = new DiscoveryKey(
                preferences.getLong(ConfigContract.KEY_DISCOVERY_VERSION_CODE, 0L),
                preferences.getLong(ConfigContract.KEY_DISCOVERY_UPDATE_TIME, 0L),
                preferences.getInt(ConfigContract.KEY_DISCOVERY_SCHEMA_REVISION, 0),
                preferences.getLong(ConfigContract.KEY_RESCAN_GENERATION, 0L)
        );
        return new ExecutionRecord(key, coverage);
    }

    record ExecutionRecord(DiscoveryKey key, ExecutionCoverage coverage) {
    }
}

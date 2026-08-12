package com.wayne.hyperaicrbypass.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.wayne.hyperaicrbypass.adapt.CoverageLayer;
import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;
import com.wayne.hyperaicrbypass.hook.ExecutionCoverage;
import com.wayne.hyperaicrbypass.hook.PreciseProgressCoverage;
import com.wayne.hyperaicrbypass.hook.BrowserHookCoverage;

import java.util.EnumMap;
import java.util.Map;

final class CoverageStore {
    private static final String PREFERENCES = "coverage_status";

    private final SharedPreferences preferences;

    CoverageStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    synchronized void report(
            Policy policy,
            CoverageLayer layer,
            long generation,
            String discoveryKey
    ) {
        String generationKey = ConfigContract.coverageKey(policy) + ".generation";
        String keyKey = ConfigContract.coverageKey(policy) + ".discovery_key";
        DiscoveryKey current = DiscoveryKey.parse(preferences.getString(keyKey, null));
        DiscoveryKey incoming = DiscoveryKey.parse(discoveryKey);
        if (incoming == null || (current != null && !acceptPolicyKey(current, incoming))) {
            return;
        }
        if (!preferences.edit()
                .putString(ConfigContract.coverageKey(policy), layer.name())
                .putLong(generationKey, generation)
                .putString(keyKey, discoveryKey)
                .commit()) {
            throw new IllegalStateException("Unable to persist coverage status");
        }
    }

    private static boolean acceptPolicyKey(DiscoveryKey current, DiscoveryKey incoming) {
        if (current.equals(incoming)) {
            return true;
        }
        if (current.versionCode() == incoming.versionCode()
                && current.lastUpdateTime() == incoming.lastUpdateTime()
                && current.schemaRevision() == incoming.schemaRevision()) {
            return incoming.rescanGeneration() > current.rescanGeneration();
        }
        return incoming.lastUpdateTime() > current.lastUpdateTime();
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

    synchronized String readPolicyDiscoveryKey(Policy policy) {
        return preferences.getString(
                ConfigContract.coverageKey(policy) + ".discovery_key", ""
        );
    }

    synchronized void reportExecution(DiscoveryKey key, ExecutionCoverage coverage) {
        String currentKey = preferences.getString(
                ConfigContract.KEY_EXECUTION_DISCOVERY_KEY, null
        );
        DiscoveryKey current = currentKey == null ? null : new DiscoveryKey(
                preferences.getLong(ConfigContract.KEY_DISCOVERY_VERSION_CODE, 0L),
                preferences.getLong(ConfigContract.KEY_DISCOVERY_UPDATE_TIME, 0L),
                preferences.getInt(ConfigContract.KEY_DISCOVERY_SCHEMA_REVISION, 0),
                preferences.getLong(ConfigContract.KEY_RESCAN_GENERATION, 0L)
        );
        if (!ExecutionCoverage.shouldAccept(current, key, coverage)) {
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

    synchronized void reportPreciseProgress(
            String discoveryKey,
            PreciseProgressCoverage coverage,
            int count,
            int expected
    ) {
        PreciseProgressRecord currentRecord = readPreciseProgress();
        if (!PreciseProgressCoverage.shouldAccept(
                DiscoveryKey.parse(currentRecord.discoveryKey()),
                currentRecord.coverage(),
                currentRecord.installedCount(),
                DiscoveryKey.parse(discoveryKey),
                coverage,
                count
        )) {
            return;
        }
        if (!preferences.edit()
                .putString(ConfigContract.KEY_PRECISE_PROGRESS_DISCOVERY_KEY, discoveryKey)
                .putString(ConfigContract.KEY_PRECISE_PROGRESS_COVERAGE, coverage.name())
                .putInt(ConfigContract.KEY_PRECISE_PROGRESS_COUNT, count)
                .putInt(ConfigContract.KEY_PRECISE_PROGRESS_EXPECTED, expected)
                .commit()) {
            throw new IllegalStateException("Unable to persist precise progress coverage");
        }
    }

    synchronized PreciseProgressRecord readPreciseProgress() {
        PreciseProgressCoverage coverage;
        try {
            coverage = PreciseProgressCoverage.valueOf(preferences.getString(
                    ConfigContract.KEY_PRECISE_PROGRESS_COVERAGE,
                    PreciseProgressCoverage.PENDING.name()
            ));
        } catch (IllegalArgumentException ignored) {
            coverage = PreciseProgressCoverage.PENDING;
        }
        return new PreciseProgressRecord(
                preferences.getString(
                        ConfigContract.KEY_PRECISE_PROGRESS_DISCOVERY_KEY, ""
                ),
                coverage,
                preferences.getInt(ConfigContract.KEY_PRECISE_PROGRESS_COUNT, 0),
                preferences.getInt(ConfigContract.KEY_PRECISE_PROGRESS_EXPECTED, 0)
        );
    }

    synchronized void reportBrowser(
            String discoveryKey,
            BrowserHookCoverage coverage,
            int count,
            int expected
    ) {
        BrowserRecord current = readBrowser();
        if (!PreciseProgressCoverage.shouldAccept(
                DiscoveryKey.parse(current.discoveryKey()),
                toPrecise(current.coverage()),
                current.installedCount(),
                DiscoveryKey.parse(discoveryKey),
                toPrecise(coverage),
                count
        )) {
            return;
        }
        if (!preferences.edit()
                .putString(ConfigContract.KEY_BROWSER_DISCOVERY_KEY, discoveryKey)
                .putString(ConfigContract.KEY_BROWSER_COVERAGE, coverage.name())
                .putInt(ConfigContract.KEY_BROWSER_HOOK_COUNT, count)
                .putInt(ConfigContract.KEY_BROWSER_HOOK_EXPECTED, expected)
                .commit()) {
            throw new IllegalStateException("Unable to persist browser coverage");
        }
    }

    synchronized BrowserRecord readBrowser() {
        BrowserHookCoverage coverage;
        try {
            coverage = BrowserHookCoverage.valueOf(preferences.getString(
                    ConfigContract.KEY_BROWSER_COVERAGE,
                    BrowserHookCoverage.PENDING.name()
            ));
        } catch (IllegalArgumentException ignored) {
            coverage = BrowserHookCoverage.PENDING;
        }
        return new BrowserRecord(
                preferences.getString(ConfigContract.KEY_BROWSER_DISCOVERY_KEY, ""),
                coverage,
                preferences.getInt(ConfigContract.KEY_BROWSER_HOOK_COUNT, 0),
                preferences.getInt(ConfigContract.KEY_BROWSER_HOOK_EXPECTED, 0)
        );
    }

    private static PreciseProgressCoverage toPrecise(BrowserHookCoverage coverage) {
        return PreciseProgressCoverage.valueOf(coverage.name());
    }

    record ExecutionRecord(DiscoveryKey key, ExecutionCoverage coverage) {
    }

    record PreciseProgressRecord(
            String discoveryKey,
            PreciseProgressCoverage coverage,
            int installedCount,
            int expectedCount
    ) {
    }

    record BrowserRecord(
            String discoveryKey,
            BrowserHookCoverage coverage,
            int installedCount,
            int expectedCount
    ) {
    }
}

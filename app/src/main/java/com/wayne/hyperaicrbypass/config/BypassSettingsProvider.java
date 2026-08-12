package com.wayne.hyperaicrbypass.config;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;

import java.util.Arrays;
import java.util.Collections;

import com.wayne.hyperaicrbypass.adapt.CoverageLayer;
import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;
import com.wayne.hyperaicrbypass.hook.ExecutionCoverage;
import com.wayne.hyperaicrbypass.hook.PreciseProgressCoverage;
import com.wayne.hyperaicrbypass.hook.BrowserHookCoverage;

public final class BypassSettingsProvider extends ContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://" + ConfigContract.AUTHORITY);

    private ConfigStore store;
    private CallerAuthorizer authorizer;
    private CoverageStore coverageStore;
    private BrowserConfigStore browserConfigStore;

    @Override
    public boolean onCreate() {
        if (getContext() == null) {
            return false;
        }
        store = new ConfigStore(getContext());
        coverageStore = new CoverageStore(getContext());
        browserConfigStore = new BrowserConfigStore(getContext());
        ApplicationInfo applicationInfo = getContext().getApplicationInfo();
        authorizer = new CallerAuthorizer(applicationInfo.uid, uid -> {
            String[] packages = getContext().getPackageManager().getPackagesForUid(uid);
            return packages == null ? Collections.emptyList() : Arrays.asList(packages);
        });
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        int callerUid = Binder.getCallingUid();
        return switch (method) {
            case ConfigContract.METHOD_GET_SNAPSHOT -> {
                require(authorizer.canReadSnapshot(callerUid), "snapshot read");
                yield BundleConfigCodec.encode(store.read());
            }
            case ConfigContract.METHOD_SET_MASTER -> {
                require(authorizer.canMutate(callerUid), "settings mutation");
                boolean enabled = requireBoolean(extras, ConfigContract.KEY_MASTER);
                yield changed(store.update(config -> config.withMaster(enabled)));
            }
            case ConfigContract.METHOD_SET_MODE -> {
                require(authorizer.canMutate(callerUid), "settings mutation");
                OperatingMode mode = OperatingMode.valueOf(
                        requireText(extras, ConfigContract.KEY_MODE)
                );
                yield changed(store.update(config -> config.withMode(mode)));
            }
            case ConfigContract.METHOD_SET_POWER_EXCEPTION -> {
                require(authorizer.canMutate(callerUid), "settings mutation");
                boolean enabled = requireBoolean(extras, ConfigContract.KEY_POWER_EXCEPTION);
                yield changed(store.update(config -> config.withPowerException(enabled)));
            }
            case ConfigContract.METHOD_SET_PROGRESS_PRECISION -> {
                require(authorizer.canMutate(callerUid), "settings mutation");
                ProgressPrecision precision = ProgressPrecision.fromStored(
                        requireText(extras, ConfigContract.KEY_PROGRESS_PRECISION)
                );
                yield changed(store.update(config -> config.withProgressPrecision(precision)));
            }
            case ConfigContract.METHOD_SET_POLICY -> {
                require(authorizer.canMutate(callerUid), "settings mutation");
                String policyKey = requireText(extras, ConfigContract.KEY_POLICY);
                boolean selected = requireBoolean(extras, ConfigContract.KEY_SELECTED);
                Policy policy = Policy.fromKey(policyKey);
                yield changed(store.update(config -> config.withPolicy(policy, selected)));
            }
            case ConfigContract.METHOD_SET_ALL -> {
                require(authorizer.canMutate(callerUid), "settings mutation");
                boolean selected = requireBoolean(extras, ConfigContract.KEY_SELECTED);
                yield changed(store.update(config -> config.withSelectAllMode(selected)));
            }
            case ConfigContract.METHOD_RESCAN -> {
                require(authorizer.canMutate(callerUid), "rescan");
                yield changed(store.update(BypassConfig::nextRescanGeneration));
            }
            case ConfigContract.METHOD_GET_BROWSER_CONFIG -> {
                require(authorizer.canReadSnapshot(callerUid), "browser config read");
                yield BrowserConfigClient.encode(browserConfigStore.read());
            }
            case ConfigContract.METHOD_SET_BROWSER_CONFIG -> {
                require(authorizer.canMutate(callerUid), "browser config mutation");
                boolean enabled = requireBoolean(extras, ConfigContract.KEY_BROWSER_ENABLED);
                String packageName = requireText(extras, ConfigContract.KEY_BROWSER_PACKAGE);
                BrowserConfig updated = browserConfigStore.update(enabled, packageName);
                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(CONTENT_URI, null);
                }
                yield BrowserConfigClient.encode(updated);
            }
            case ConfigContract.METHOD_REPORT_COVERAGE -> {
                require(authorizer.canReportCoverage(callerUid), "coverage report");
                Policy policy = Policy.fromKey(requireText(extras, ConfigContract.KEY_POLICY));
                CoverageLayer layer = CoverageLayer.valueOf(
                        requireText(extras, ConfigContract.KEY_LAYER)
                );
                long generation = requireNonNegativeLong(extras, ConfigContract.KEY_GENERATION);
                String discoveryKey = requireText(
                        extras, ConfigContract.KEY_EXECUTION_DISCOVERY_KEY
                );
                coverageStore.report(policy, layer, generation, discoveryKey);
                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(CONTENT_URI, null);
                }
                yield Bundle.EMPTY;
            }
            case ConfigContract.METHOD_REPORT_EXECUTION_COVERAGE -> {
                require(authorizer.canReportCoverage(callerUid), "execution coverage report");
                DiscoveryKey key = new DiscoveryKey(
                        requireNonNegativeLong(
                                extras, ConfigContract.KEY_DISCOVERY_VERSION_CODE
                        ),
                        requireNonNegativeLong(
                                extras, ConfigContract.KEY_DISCOVERY_UPDATE_TIME
                        ),
                        requireNonNegativeInt(
                                extras, ConfigContract.KEY_DISCOVERY_SCHEMA_REVISION
                        ),
                        requireNonNegativeLong(extras, ConfigContract.KEY_RESCAN_GENERATION)
                );
                ExecutionCoverage coverage = ExecutionCoverage.valueOf(
                        requireText(extras, ConfigContract.KEY_EXECUTION_COVERAGE)
                );
                coverageStore.reportExecution(key, coverage);
                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(CONTENT_URI, null);
                }
                yield Bundle.EMPTY;
            }
            case ConfigContract.METHOD_REPORT_PRECISE_PROGRESS_COVERAGE -> {
                require(authorizer.canReportCoverage(callerUid),
                        "precise progress coverage report");
                String discoveryKey = requireText(
                        extras, ConfigContract.KEY_PRECISE_PROGRESS_DISCOVERY_KEY
                );
                PreciseProgressCoverage coverage = PreciseProgressCoverage.valueOf(
                        requireText(extras, ConfigContract.KEY_PRECISE_PROGRESS_COVERAGE)
                );
                int count = requireNonNegativeInt(
                        extras, ConfigContract.KEY_PRECISE_PROGRESS_COUNT
                );
                int expected = requireNonNegativeInt(
                        extras, ConfigContract.KEY_PRECISE_PROGRESS_EXPECTED
                );
                coverageStore.reportPreciseProgress(discoveryKey, coverage, count, expected);
                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(CONTENT_URI, null);
                }
                yield Bundle.EMPTY;
            }
            case ConfigContract.METHOD_REPORT_BROWSER_COVERAGE -> {
                require(authorizer.canReportCoverage(callerUid), "browser coverage report");
                coverageStore.reportBrowser(
                        requireText(extras, ConfigContract.KEY_BROWSER_DISCOVERY_KEY),
                        BrowserHookCoverage.valueOf(
                                requireText(extras, ConfigContract.KEY_BROWSER_COVERAGE)
                        ),
                        requireNonNegativeInt(extras, ConfigContract.KEY_BROWSER_HOOK_COUNT),
                        requireNonNegativeInt(extras, ConfigContract.KEY_BROWSER_HOOK_EXPECTED)
                );
                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(CONTENT_URI, null);
                }
                yield Bundle.EMPTY;
            }
            case ConfigContract.METHOD_GET_COVERAGE -> {
                require(authorizer.canReadSnapshot(callerUid), "coverage read");
                Bundle response = new Bundle();
                for (var entry : coverageStore.read().entrySet()) {
                    response.putString(
                            ConfigContract.coverageKey(entry.getKey()), entry.getValue().name()
                    );
                    response.putString(
                            ConfigContract.coverageDiscoveryKey(entry.getKey()),
                            coverageStore.readPolicyDiscoveryKey(entry.getKey())
                    );
                }
                CoverageStore.ExecutionRecord execution = coverageStore.readExecution();
                response.putString(
                        ConfigContract.KEY_EXECUTION_COVERAGE,
                        execution.coverage().name()
                );
                response.putString(
                        ConfigContract.KEY_EXECUTION_DISCOVERY_KEY,
                        execution.key().stableValue()
                );
                response.putLong(
                        ConfigContract.KEY_DISCOVERY_VERSION_CODE,
                        execution.key().versionCode()
                );
                response.putLong(
                        ConfigContract.KEY_DISCOVERY_UPDATE_TIME,
                        execution.key().lastUpdateTime()
                );
                response.putInt(
                        ConfigContract.KEY_DISCOVERY_SCHEMA_REVISION,
                        execution.key().schemaRevision()
                );
                response.putLong(
                        ConfigContract.KEY_RESCAN_GENERATION,
                        execution.key().rescanGeneration()
                );
                CoverageStore.PreciseProgressRecord precise =
                        coverageStore.readPreciseProgress();
                response.putString(ConfigContract.KEY_PRECISE_PROGRESS_DISCOVERY_KEY,
                        precise.discoveryKey());
                response.putString(ConfigContract.KEY_PRECISE_PROGRESS_COVERAGE,
                        precise.coverage().name());
                response.putInt(ConfigContract.KEY_PRECISE_PROGRESS_COUNT,
                        precise.installedCount());
                response.putInt(ConfigContract.KEY_PRECISE_PROGRESS_EXPECTED,
                        precise.expectedCount());
                CoverageStore.BrowserRecord browser = coverageStore.readBrowser();
                response.putString(ConfigContract.KEY_BROWSER_DISCOVERY_KEY,
                        browser.discoveryKey());
                response.putString(ConfigContract.KEY_BROWSER_COVERAGE,
                        browser.coverage().name());
                response.putInt(ConfigContract.KEY_BROWSER_HOOK_COUNT,
                        browser.installedCount());
                response.putInt(ConfigContract.KEY_BROWSER_HOOK_EXPECTED,
                        browser.expectedCount());
                yield response;
            }
            default -> throw new IllegalArgumentException("Unknown provider method: " + method);
        };
    }

    private Bundle changed(BypassConfig config) {
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        }
        return BundleConfigCodec.encode(config);
    }

    private static void require(boolean allowed, String operation) {
        if (!allowed) {
            throw new SecurityException("Caller is not authorized for " + operation);
        }
    }

    private static boolean requireBoolean(Bundle extras, String key) {
        if (extras == null || !extras.containsKey(key) || !(extras.get(key) instanceof Boolean)) {
            throw new IllegalArgumentException(key + " must be a boolean");
        }
        return extras.getBoolean(key);
    }

    private static String requireText(Bundle extras, String key) {
        if (extras == null || !(extras.get(key) instanceof String value)) {
            throw new IllegalArgumentException(key + " must be text");
        }
        return ConfigContract.requireShortText(value, key);
    }

    private static long requireNonNegativeLong(Bundle extras, String key) {
        if (extras == null || !(extras.get(key) instanceof Number number)) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        long value = number.longValue();
        if (value < 0) {
            throw new IllegalArgumentException(key + " must not be negative");
        }
        return value;
    }

    private static int requireNonNegativeInt(Bundle extras, String key) {
        long value = requireNonNegativeLong(extras, key);
        if (value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(key + " exceeds integer range");
        }
        return (int) value;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        throw new UnsupportedOperationException("Use ContentProvider.call");
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Use ContentProvider.call");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Use ContentProvider.call");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Use ContentProvider.call");
    }
}

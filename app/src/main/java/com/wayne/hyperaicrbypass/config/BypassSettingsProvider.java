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

public final class BypassSettingsProvider extends ContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://" + ConfigContract.AUTHORITY);

    private ConfigStore store;
    private CallerAuthorizer authorizer;
    private CoverageStore coverageStore;

    @Override
    public boolean onCreate() {
        if (getContext() == null) {
            return false;
        }
        store = new ConfigStore(getContext());
        coverageStore = new CoverageStore(getContext());
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
            case ConfigContract.METHOD_REPORT_COVERAGE -> {
                require(authorizer.canReportCoverage(callerUid), "coverage report");
                Policy policy = Policy.fromKey(requireText(extras, ConfigContract.KEY_POLICY));
                CoverageLayer layer = CoverageLayer.valueOf(
                        requireText(extras, ConfigContract.KEY_LAYER)
                );
                long generation = requireNonNegativeLong(extras, ConfigContract.KEY_GENERATION);
                coverageStore.report(policy, layer, generation);
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
                }
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

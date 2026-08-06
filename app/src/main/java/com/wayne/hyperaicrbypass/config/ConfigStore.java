package com.wayne.hyperaicrbypass.config;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.EnumSet;
import java.util.function.UnaryOperator;

public final class ConfigStore {
    private static final String PREFERENCES = "bypass_settings";

    private final SharedPreferences preferences;

    public ConfigStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public synchronized BypassConfig read() {
        BypassConfig defaults = BypassConfig.defaults();
        boolean legacyMaster = preferences.getBoolean(
                ConfigContract.KEY_MASTER, defaults.isMasterEnabled()
        );
        String storedMode = preferences.contains(ConfigContract.KEY_MODE)
                ? preferences.getString(ConfigContract.KEY_MODE, null)
                : null;
        OperatingMode mode = OperatingMode.fromStored(storedMode, legacyMaster);
        ProgressPrecision progressPrecision = ProgressPrecision.fromStored(preferences.getString(
                ConfigContract.KEY_PROGRESS_PRECISION,
                defaults.getProgressPrecision().name()
        ));
        ProgressPrecision lastNonOriginalPrecision = ProgressPrecision.fromStored(
                preferences.getString(
                        ConfigContract.KEY_LAST_NON_ORIGINAL_PRECISION,
                        defaults.getLastNonOriginalPrecision().name()
                )
        );
        EnumSet<Policy> selected = EnumSet.noneOf(Policy.class);
        for (Policy policy : Policy.values()) {
            if (preferences.getBoolean(ConfigContract.policyKey(policy), true)) {
                selected.add(policy);
            }
        }
        return BypassConfig.create(
                mode,
                preferences.getBoolean(ConfigContract.KEY_POWER_EXCEPTION, false),
                progressPrecision,
                lastNonOriginalPrecision,
                preferences.getBoolean(
                        ConfigContract.KEY_SELECT_ALL_MODE, defaults.isSelectAllMode()
                ),
                selected,
                preferences.getLong(ConfigContract.KEY_CONFIG_REVISION, 0L),
                preferences.getLong(ConfigContract.KEY_RESCAN_GENERATION, 0L)
        );
    }

    public synchronized BypassConfig update(UnaryOperator<BypassConfig> mutation) {
        BypassConfig updated = mutation.apply(read());
        SharedPreferences.Editor editor = preferences.edit()
                .putBoolean(ConfigContract.KEY_MASTER, updated.isMasterEnabled())
                .putString(ConfigContract.KEY_MODE, updated.getMode().name())
                .putBoolean(ConfigContract.KEY_POWER_EXCEPTION,
                        updated.isPowerExceptionEnabled())
                .putString(ConfigContract.KEY_PROGRESS_PRECISION,
                        updated.getProgressPrecision().name())
                .putString(ConfigContract.KEY_LAST_NON_ORIGINAL_PRECISION,
                        updated.getLastNonOriginalPrecision().name())
                .putBoolean(ConfigContract.KEY_SELECT_ALL_MODE, updated.isSelectAllMode())
                .putLong(ConfigContract.KEY_CONFIG_REVISION, updated.getConfigRevision())
                .putLong(ConfigContract.KEY_RESCAN_GENERATION, updated.getRescanGeneration());
        for (Policy policy : Policy.values()) {
            editor.putBoolean(ConfigContract.policyKey(policy), updated.isSelected(policy));
        }
        if (!editor.commit()) {
            throw new IllegalStateException("Unable to persist bypass settings");
        }
        return updated;
    }
}

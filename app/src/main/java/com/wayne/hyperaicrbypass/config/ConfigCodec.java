package com.wayne.hyperaicrbypass.config;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ConfigCodec {
    private ConfigCodec() {
    }

    public static Map<String, Object> encode(BypassConfig config) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(ConfigContract.KEY_MASTER, config.isMasterEnabled());
        values.put(ConfigContract.KEY_SELECT_ALL_MODE, config.isSelectAllMode());
        values.put(ConfigContract.KEY_CONFIG_REVISION, config.getConfigRevision());
        values.put(ConfigContract.KEY_RESCAN_GENERATION, config.getRescanGeneration());
        for (Policy policy : Policy.values()) {
            values.put(ConfigContract.policyKey(policy), config.isSelected(policy));
        }
        return Collections.unmodifiableMap(values);
    }

    public static BypassConfig decode(Map<String, ?> values) {
        Set<String> expectedKeys = expectedKeys();
        if (!values.keySet().equals(expectedKeys)) {
            throw new IllegalArgumentException("Config snapshot keys do not match the contract");
        }

        boolean master = requireBoolean(values, ConfigContract.KEY_MASTER);
        boolean selectAllMode = requireBoolean(values, ConfigContract.KEY_SELECT_ALL_MODE);
        long configRevision = requireNonNegativeLong(values, ConfigContract.KEY_CONFIG_REVISION);
        long rescanGeneration = requireNonNegativeLong(values, ConfigContract.KEY_RESCAN_GENERATION);
        EnumSet<Policy> selected = EnumSet.noneOf(Policy.class);
        for (Policy policy : Policy.values()) {
            if (requireBoolean(values, ConfigContract.policyKey(policy))) {
                selected.add(policy);
            }
        }
        return BypassConfig.create(
                master, selectAllMode, selected, configRevision, rescanGeneration
        );
    }

    private static Set<String> expectedKeys() {
        Set<String> keys = new java.util.LinkedHashSet<>();
        keys.add(ConfigContract.KEY_MASTER);
        keys.add(ConfigContract.KEY_SELECT_ALL_MODE);
        keys.add(ConfigContract.KEY_CONFIG_REVISION);
        keys.add(ConfigContract.KEY_RESCAN_GENERATION);
        for (Policy policy : Policy.values()) {
            keys.add(ConfigContract.policyKey(policy));
        }
        return keys;
    }

    private static boolean requireBoolean(Map<String, ?> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException(key + " must be a boolean");
        }
        return (Boolean) value;
    }

    private static long requireNonNegativeLong(Map<String, ?> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        long result = number.longValue();
        if (result < 0) {
            throw new IllegalArgumentException(key + " must not be negative");
        }
        return result;
    }
}

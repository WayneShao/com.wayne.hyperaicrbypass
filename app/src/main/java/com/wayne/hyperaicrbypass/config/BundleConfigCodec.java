package com.wayne.hyperaicrbypass.config;

import android.os.Bundle;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BundleConfigCodec {
    private BundleConfigCodec() {
    }

    public static Bundle encode(BypassConfig config) {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, Object> entry : ConfigCodec.encode(config).entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean booleanValue) {
                bundle.putBoolean(entry.getKey(), booleanValue);
            } else if (value instanceof Number numberValue) {
                bundle.putLong(entry.getKey(), numberValue.longValue());
            } else if (value instanceof String stringValue) {
                bundle.putString(entry.getKey(), stringValue);
            } else {
                throw new IllegalArgumentException("Unsupported config value: " + entry.getKey());
            }
        }
        return bundle;
    }

    public static BypassConfig decode(Bundle bundle) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : bundle.keySet()) {
            values.put(key, bundle.get(key));
        }
        return ConfigCodec.decode(values);
    }
}

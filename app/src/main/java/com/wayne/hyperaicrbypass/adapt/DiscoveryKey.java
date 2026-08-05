package com.wayne.hyperaicrbypass.adapt;

public record DiscoveryKey(
        long versionCode,
        long lastUpdateTime,
        int schemaRevision,
        long rescanGeneration
) {
    public String stableValue() {
        return versionCode + ":" + lastUpdateTime + ":" + schemaRevision + ":" + rescanGeneration;
    }
}

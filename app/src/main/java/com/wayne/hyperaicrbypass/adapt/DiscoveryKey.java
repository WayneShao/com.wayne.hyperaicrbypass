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

    public static DiscoveryKey parse(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split(":", -1);
        if (parts.length != 4) {
            return null;
        }
        try {
            return new DiscoveryKey(
                    Long.parseLong(parts[0]),
                    Long.parseLong(parts[1]),
                    Integer.parseInt(parts[2]),
                    Long.parseLong(parts[3])
            );
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

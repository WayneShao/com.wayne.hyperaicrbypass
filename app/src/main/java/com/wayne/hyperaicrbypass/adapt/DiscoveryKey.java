package com.wayne.hyperaicrbypass.adapt;

public record DiscoveryKey(
        long versionCode,
        long lastUpdateTime,
        int schemaRevision,
        long rescanGeneration
) {
    public boolean isNewerThan(DiscoveryKey other) {
        if (other == null) {
            return true;
        }
        int updateOrder = Long.compare(lastUpdateTime, other.lastUpdateTime);
        if (updateOrder != 0) {
            return updateOrder > 0;
        }
        int versionOrder = Long.compare(versionCode, other.versionCode);
        if (versionOrder != 0) {
            return versionOrder > 0;
        }
        int schemaOrder = Integer.compare(schemaRevision, other.schemaRevision);
        if (schemaOrder != 0) {
            return schemaOrder > 0;
        }
        return rescanGeneration > other.rescanGeneration;
    }

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

package com.wayne.hyperaicrbypass.adapt;

import com.wayne.hyperaicrbypass.config.Policy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CoverageAggregator {
    private CoverageAggregator() {
    }

    public static Map<Policy, CoverageReport> forKey(
            DiscoveryKey key,
            List<CoverageReport> reports
    ) {
        EnumMap<Policy, CoverageReport> result = new EnumMap<>(Policy.class);
        for (Policy policy : Policy.values()) {
            result.put(policy, CoverageReport.pending(policy, key));
        }
        for (CoverageReport report : reports) {
            if (!report.discoveryKey().equals(key)) {
                continue;
            }
            CoverageReport current = result.get(report.policy());
            if (rank(report.layer()) >= rank(current.layer())) {
                result.put(report.policy(), report);
            }
        }
        return result;
    }

    public static int successCount(Map<Policy, CoverageReport> reports) {
        return (int) reports.values().stream()
                .filter(report -> report.layer().isSuccessful())
                .count();
    }

    private static int rank(CoverageLayer layer) {
        return switch (layer) {
            case PENDING -> 0;
            case UNAVAILABLE -> 1;
            case PARTIAL -> 2;
            case FALLBACK -> 3;
            case SEMANTIC -> 4;
            case EXACT -> 5;
        };
    }
}

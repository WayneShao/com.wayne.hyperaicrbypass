package com.wayne.hyperaicrbypass.adapt;

import com.wayne.hyperaicrbypass.config.Policy;

import java.util.Objects;

public final class CoverageReport {
    private final Policy policy;
    private final CoverageLayer layer;
    private final String descriptor;
    private final String processName;
    private final DiscoveryKey discoveryKey;
    private final long configRevision;
    private final boolean registrationSucceeded;
    private final String failureReason;

    public CoverageReport(
            Policy policy,
            CoverageLayer layer,
            String descriptor,
            String processName,
            DiscoveryKey discoveryKey,
            long configRevision,
            boolean registrationSucceeded,
            String failureReason
    ) {
        this.policy = Objects.requireNonNull(policy);
        this.layer = Objects.requireNonNull(layer);
        this.descriptor = descriptor == null ? "" : descriptor;
        this.processName = Objects.requireNonNull(processName);
        this.discoveryKey = Objects.requireNonNull(discoveryKey);
        this.configRevision = configRevision;
        this.registrationSucceeded = registrationSucceeded;
        this.failureReason = failureReason == null ? "" : failureReason;
        if (layer.isSuccessful() && (!registrationSucceeded || this.descriptor.isBlank())) {
            throw new IllegalArgumentException(
                    "Successful coverage requires a registered descriptor"
            );
        }
        if (!layer.isSuccessful() && registrationSucceeded) {
            throw new IllegalArgumentException("Non-success coverage cannot be registered");
        }
    }

    public Policy policy() {
        return policy;
    }

    public CoverageLayer layer() {
        return layer;
    }

    public String descriptor() {
        return descriptor;
    }

    public String processName() {
        return processName;
    }

    public DiscoveryKey discoveryKey() {
        return discoveryKey;
    }

    public long configRevision() {
        return configRevision;
    }

    public boolean registrationSucceeded() {
        return registrationSucceeded;
    }

    public String failureReason() {
        return failureReason;
    }

    public static CoverageReport success(
            Policy policy,
            CoverageLayer layer,
            String descriptor,
            String processName,
            DiscoveryKey key,
            long configRevision
    ) {
        if (!layer.isSuccessful()) {
            throw new IllegalArgumentException("Not a success layer: " + layer);
        }
        return new CoverageReport(
                policy, layer, descriptor, processName, key, configRevision, true, ""
        );
    }

    public static CoverageReport failure(
            Policy policy,
            CoverageLayer layer,
            String processName,
            DiscoveryKey key,
            long configRevision,
            String reason
    ) {
        return new CoverageReport(
                policy, layer, "", processName, key, configRevision, false, reason
        );
    }

    public static CoverageReport pending(Policy policy, DiscoveryKey key) {
        return failure(policy, CoverageLayer.PENDING, "", key, 0, "pending");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CoverageReport report)) {
            return false;
        }
        return configRevision == report.configRevision
                && registrationSucceeded == report.registrationSucceeded
                && policy == report.policy
                && layer == report.layer
                && descriptor.equals(report.descriptor)
                && processName.equals(report.processName)
                && discoveryKey.equals(report.discoveryKey)
                && failureReason.equals(report.failureReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                policy, layer, descriptor, processName, discoveryKey,
                configRevision, registrationSucceeded, failureReason
        );
    }
}

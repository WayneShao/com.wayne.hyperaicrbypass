package com.wayne.hyperaicrbypass.hook;

import com.wayne.hyperaicrbypass.config.Policy;

import java.util.List;

final class SemanticDiscoveryPolicy {
    private SemanticDiscoveryPolicy() {
    }

    static boolean needsDiscovery(List<HookSpec> missingExact) {
        return missingExact.stream()
                .anyMatch(spec -> spec.policy() != Policy.TASK_CONSTRAINTS);
    }
}

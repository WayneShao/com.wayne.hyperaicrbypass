package com.wayne.hyperaicrbypass.hook;

import java.util.List;

final class SemanticDiscoveryPolicy {
    private SemanticDiscoveryPolicy() {
    }

    static boolean needsDiscovery(List<HookSpec> missingExact) {
        return missingExact.stream()
                .anyMatch(spec -> spec.className().startsWith("com.xiaomi.aicr."));
    }
}

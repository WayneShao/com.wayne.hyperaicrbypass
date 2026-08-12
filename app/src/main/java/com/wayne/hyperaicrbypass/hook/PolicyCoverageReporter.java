package com.wayne.hyperaicrbypass.hook;

import android.content.Context;
import android.os.Bundle;

import com.wayne.hyperaicrbypass.adapt.CoverageLayer;
import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;
import com.wayne.hyperaicrbypass.config.BypassSettingsProvider;
import com.wayne.hyperaicrbypass.config.ConfigContract;
import com.wayne.hyperaicrbypass.config.Policy;

import java.util.EnumMap;
import java.util.Map;

import com.wayne.hyperaicrbypass.xposed.ModernXposed;

public final class PolicyCoverageReporter {
    private static final String TAG = "HyperAICRBypass";

    private final Context context;
    private final Map<Policy, Integer> expectedCounts;

    public PolicyCoverageReporter(Context context, AicrVersionBranch branch) {
        this.context = context;
        EnumMap<Policy, Integer> expected = new EnumMap<>(Policy.class);
        for (HookSpec spec : ExactHookCatalog.aicrSpecs(branch)) {
            expected.merge(spec.policy(), 1, Integer::sum);
        }
        expectedCounts = Map.copyOf(expected);
    }

    public void report(
            Map<Policy, Integer> exactCounts,
            Map<Policy, Integer> semanticCounts,
            DiscoveryKey key
    ) {
        for (Policy policy : Policy.values()) {
            int expected = expectedCounts.getOrDefault(policy, 1);
            int exact = exactCounts.getOrDefault(policy, 0);
            int semantic = semanticCounts.getOrDefault(policy, 0);
            int total = exact + semantic;
            CoverageLayer layer;
            if (exact >= expected) {
                layer = CoverageLayer.EXACT;
            } else if (total >= expected && semantic > 0) {
                layer = CoverageLayer.SEMANTIC;
            } else if (total > 0) {
                layer = CoverageLayer.PARTIAL;
            } else {
                layer = CoverageLayer.UNAVAILABLE;
            }
            Bundle extras = new Bundle();
            extras.putString(ConfigContract.KEY_POLICY, policy.getKey());
            extras.putString(ConfigContract.KEY_LAYER, layer.name());
            extras.putLong(ConfigContract.KEY_GENERATION, key.rescanGeneration());
            extras.putString(ConfigContract.KEY_EXECUTION_DISCOVERY_KEY, key.stableValue());
            try {
                context.getContentResolver().call(
                        BypassSettingsProvider.CONTENT_URI,
                        ConfigContract.METHOD_REPORT_COVERAGE,
                        null,
                        extras
                );
            } catch (RuntimeException error) {
                ModernXposed.log(TAG + ": coverage report failed " + policy.getKey()
                        + " -> " + error);
            }
        }
    }
}

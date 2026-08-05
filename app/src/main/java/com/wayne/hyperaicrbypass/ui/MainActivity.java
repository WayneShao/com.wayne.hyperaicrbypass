package com.wayne.hyperaicrbypass.ui;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.wayne.hyperaicrbypass.R;
import com.wayne.hyperaicrbypass.HyperAicrApplication;
import com.wayne.hyperaicrbypass.adapt.CoverageLayer;
import com.wayne.hyperaicrbypass.config.BundleConfigCodec;
import com.wayne.hyperaicrbypass.config.BypassConfig;
import com.wayne.hyperaicrbypass.config.BypassSettingsProvider;
import com.wayne.hyperaicrbypass.config.ConfigContract;
import com.wayne.hyperaicrbypass.config.Policy;

import java.util.EnumMap;

public final class MainActivity extends Activity {
    private static final long ACTIVATION_TIMEOUT_MS = 1_000L;

    private final Handler activationHandler = new Handler(Looper.getMainLooper());
    private final HyperAicrApplication.ActivationListener activationListener = active -> {
        if (active) {
            runOnUiThread(this::showSettings);
        }
    };
    private final Runnable activationTimeout = this::enforceActivation;
    private Switch masterSwitch;
    private Switch launcherIconSwitch;
    private Switch selectAllSwitch;
    private TextView selectionSummary;
    private TextView versionSummary;
    private TextView rescanSummary;
    private LinearLayout policyContainer;
    private LauncherIconController launcherIconController;
    private final EnumMap<Policy, Switch> policySwitches = new EnumMap<>(Policy.class);
    private final EnumMap<Policy, TextView> coverageViews = new EnumMap<>(Policy.class);
    private boolean rendering;
    private boolean settingsVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HyperAicrApplication.addActivationListener(activationListener);
        if (HyperAicrApplication.isModuleActive()) {
            showSettings();
        } else {
            activationHandler.postDelayed(activationTimeout, ACTIVATION_TIMEOUT_MS);
        }
    }

    private void showSettings() {
        if (settingsVisible || isFinishing()) {
            return;
        }
        settingsVisible = true;
        activationHandler.removeCallbacks(activationTimeout);
        setContentView(R.layout.activity_main);
        bindViews();
        createPolicyRows();
        bindActions();
    }

    private void enforceActivation() {
        if (HyperAicrApplication.isModuleActive()) {
            showSettings();
            return;
        }
        Toast.makeText(this, R.string.module_not_active, Toast.LENGTH_SHORT).show();
        finishAndRemoveTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (settingsVisible) {
            refresh();
        }
    }

    @Override
    protected void onDestroy() {
        activationHandler.removeCallbacks(activationTimeout);
        HyperAicrApplication.removeActivationListener(activationListener);
        super.onDestroy();
    }

    private void bindViews() {
        masterSwitch = findViewById(R.id.master_switch);
        launcherIconSwitch = findViewById(R.id.launcher_icon_switch);
        selectAllSwitch = findViewById(R.id.select_all_switch);
        selectionSummary = findViewById(R.id.selection_summary);
        versionSummary = findViewById(R.id.version_summary);
        rescanSummary = findViewById(R.id.rescan_summary);
        policyContainer = findViewById(R.id.policy_container);
        launcherIconController = new LauncherIconController(this);
    }

    private void createPolicyRows() {
        for (Policy policy : Policy.values()) {
            View row = getLayoutInflater().inflate(R.layout.policy_row, policyContainer, false);
            TextView title = row.findViewById(R.id.policy_title);
            TextView detail = row.findViewById(R.id.policy_detail);
            TextView coverage = row.findViewById(R.id.policy_coverage);
            Switch toggle = row.findViewById(R.id.policy_switch);
            title.setText(policyTitle(policy));
            detail.setText(policyDetail(policy));
            toggle.setContentDescription(getString(R.string.policy_toggle_description, title.getText()));
            toggle.setOnCheckedChangeListener((button, selected) -> {
                if (!rendering) {
                    mutatePolicy(policy, selected);
                }
            });
            policySwitches.put(policy, toggle);
            coverageViews.put(policy, coverage);
            policyContainer.addView(row);
        }
    }

    private void bindActions() {
        masterSwitch.setOnCheckedChangeListener((button, enabled) -> {
            if (!rendering) {
                Bundle extras = new Bundle();
                extras.putBoolean(ConfigContract.KEY_MASTER, enabled);
                mutate(ConfigContract.METHOD_SET_MASTER, extras);
            }
        });
        launcherIconSwitch.setOnCheckedChangeListener((button, visible) -> {
            if (rendering) {
                return;
            }
            try {
                launcherIconController.setVisible(visible);
            } catch (RuntimeException error) {
                Toast.makeText(
                        this, R.string.launcher_icon_write_failed, Toast.LENGTH_SHORT
                ).show();
                renderLauncherIconState();
            }
        });
        selectAllSwitch.setOnCheckedChangeListener((button, selected) -> {
            if (!rendering) {
                Bundle extras = new Bundle();
                extras.putBoolean(ConfigContract.KEY_SELECTED, selected);
                mutate(ConfigContract.METHOD_SET_ALL, extras);
            }
        });
        Button rescan = findViewById(R.id.rescan_button);
        rescan.setOnClickListener(view -> mutate(ConfigContract.METHOD_RESCAN, null));
    }

    private void mutatePolicy(Policy policy, boolean selected) {
        Bundle extras = new Bundle();
        extras.putString(ConfigContract.KEY_POLICY, policy.getKey());
        extras.putBoolean(ConfigContract.KEY_SELECTED, selected);
        mutate(ConfigContract.METHOD_SET_POLICY, extras);
    }

    private void mutate(String method, Bundle extras) {
        try {
            Bundle response = getContentResolver().call(
                    BypassSettingsProvider.CONTENT_URI, method, null, extras);
            render(BundleConfigCodec.decode(response));
        } catch (RuntimeException error) {
            Toast.makeText(this, R.string.settings_write_failed, Toast.LENGTH_SHORT).show();
            refresh();
        }
    }

    private void refresh() {
        try {
            Bundle response = getContentResolver().call(
                    BypassSettingsProvider.CONTENT_URI,
                    ConfigContract.METHOD_GET_SNAPSHOT,
                    null,
                    null
            );
            render(BundleConfigCodec.decode(response));
        } catch (RuntimeException error) {
            Toast.makeText(this, R.string.settings_read_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void render(BypassConfig config) {
        SettingsState state = new SettingsState(config, findAicrVersion(), readCoverage());
        rendering = true;
        masterSwitch.setChecked(state.isMasterEnabled());
        launcherIconSwitch.setChecked(launcherIconController.isVisible());
        selectAllSwitch.setChecked(state.isSelectAllMode());
        selectionSummary.setText(state.selectionSummary());
        versionSummary.setText(state.versionSummary());
        rescanSummary.setText(state.rescanSummary());
        for (Policy policy : Policy.values()) {
            policySwitches.get(policy).setChecked(state.isPolicySelected(policy));
            policySwitches.get(policy).setEnabled(state.isPolicyEditable(policy));
            coverageViews.get(policy).setText(state.coverageLabel(policy));
        }
        rendering = false;
    }

    private void renderLauncherIconState() {
        rendering = true;
        launcherIconSwitch.setChecked(launcherIconController.isVisible());
        rendering = false;
    }

    private EnumMap<Policy, CoverageLayer> readCoverage() {
        EnumMap<Policy, CoverageLayer> result = new EnumMap<>(Policy.class);
        try {
            Bundle response = getContentResolver().call(
                    BypassSettingsProvider.CONTENT_URI,
                    ConfigContract.METHOD_GET_COVERAGE,
                    null,
                    null
            );
            if (response == null) {
                return result;
            }
            for (Policy policy : Policy.values()) {
                String value = response.getString(ConfigContract.coverageKey(policy));
                if (value != null) {
                    result.put(policy, CoverageLayer.valueOf(value));
                }
            }
        } catch (RuntimeException ignored) {
            // Pending is safer than disabling a row when coverage cannot be read.
        }
        return result;
    }

    private String findAicrVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.xiaomi.aicr", 0);
            return info.versionName == null ? Long.toString(info.getLongVersionCode()) : info.versionName;
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private int policyTitle(Policy policy) {
        return switch (policy) {
            case TEMPERATURE -> R.string.policy_temperature;
            case CHARGING -> R.string.policy_charging;
            case POWER -> R.string.policy_power;
            case SCREEN_IDLE -> R.string.policy_screen_idle;
            case MIGRATION -> R.string.policy_migration;
            case DAILY_COUNT -> R.string.policy_daily_count;
            case DURATION -> R.string.policy_duration;
            case RUN_GAP -> R.string.policy_run_gap;
            case OVERLOAD -> R.string.policy_overload;
            case TASK_CONSTRAINTS -> R.string.policy_task_constraints;
            case AI_UI_CAPABILITY -> R.string.policy_ai_ui_capability;
        };
    }

    private int policyDetail(Policy policy) {
        return switch (policy) {
            case TEMPERATURE -> R.string.policy_temperature_detail;
            case CHARGING -> R.string.policy_charging_detail;
            case POWER -> R.string.policy_power_detail;
            case SCREEN_IDLE -> R.string.policy_screen_idle_detail;
            case MIGRATION -> R.string.policy_migration_detail;
            case DAILY_COUNT -> R.string.policy_daily_count_detail;
            case DURATION -> R.string.policy_duration_detail;
            case RUN_GAP -> R.string.policy_run_gap_detail;
            case OVERLOAD -> R.string.policy_overload_detail;
            case TASK_CONSTRAINTS -> R.string.policy_task_constraints_detail;
            case AI_UI_CAPABILITY -> R.string.policy_ai_ui_capability_detail;
        };
    }
}

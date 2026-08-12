package com.wayne.hyperaicrbypass.ui;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.wayne.hyperaicrbypass.R;
import com.wayne.hyperaicrbypass.HyperAicrApplication;
import com.wayne.hyperaicrbypass.adapt.CoverageLayer;
import com.wayne.hyperaicrbypass.adapt.DiscoveryKey;
import com.wayne.hyperaicrbypass.config.BundleConfigCodec;
import com.wayne.hyperaicrbypass.config.BrowserConfig;
import com.wayne.hyperaicrbypass.config.BrowserConfigClient;
import com.wayne.hyperaicrbypass.config.BypassConfig;
import com.wayne.hyperaicrbypass.config.BypassSettingsProvider;
import com.wayne.hyperaicrbypass.config.ConfigContract;
import com.wayne.hyperaicrbypass.config.Policy;
import com.wayne.hyperaicrbypass.config.OperatingMode;
import com.wayne.hyperaicrbypass.config.ProgressPrecision;
import com.wayne.hyperaicrbypass.hook.ExecutionCoverage;
import com.wayne.hyperaicrbypass.hook.ExecutionCoverageReporter;
import com.wayne.hyperaicrbypass.hook.PreciseProgressCoverage;
import com.wayne.hyperaicrbypass.hook.ExternalPowerMonitor;
import com.wayne.hyperaicrbypass.hook.CopyWebsiteBrowser;
import com.wayne.hyperaicrbypass.hook.BrowserHookCoverage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public final class MainActivity extends Activity {
    private static final long ACTIVATION_TIMEOUT_MS = 1_000L;

    private final Handler activationHandler = new Handler(Looper.getMainLooper());
    private final HyperAicrApplication.ActivationListener activationListener = active -> {
        if (active) {
            runOnUiThread(this::showSettings);
        }
    };
    private final Runnable activationTimeout = this::enforceActivation;
    private final ContentObserver settingsObserver = new ContentObserver(activationHandler) {
        @Override
        public void onChange(boolean selfChange) {
            if (settingsVisible && !isFinishing()) {
                refresh();
            }
        }
    };
    private Switch masterSwitch;
    private Switch powerSaveSwitch;
    private Switch powerExceptionSwitch;
    private Switch preciseProgressSwitch;
    private Switch launcherIconSwitch;
    private Switch copyBrowserSwitch;
    private Switch selectAllSwitch;
    private TextView selectionSummary;
    private TextView versionSummary;
    private TextView rescanSummary;
    private TextView runtimeSummary;
    private TextView preciseProgressStatus;
    private TextView copyBrowserStatus;
    private View powerExceptionRow;
    private RadioGroup precisionSelector;
    private Spinner copyBrowserSelector;
    private View copyBrowserSelectorRow;
    private LinearLayout policyContainer;
    private LauncherIconController launcherIconController;
    private final EnumMap<Policy, Switch> policySwitches = new EnumMap<>(Policy.class);
    private final EnumMap<Policy, TextView> coverageViews = new EnumMap<>(Policy.class);
    private boolean rendering;
    private boolean settingsVisible;
    private BypassConfig lastConfig;
    private ExternalPowerMonitor powerMonitor;
    private boolean observingSettings;
    private List<CopyWebsiteBrowser.BrowserChoice> browserChoices = List.of();

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
        applySystemBarInsets();
        bindViews();
        createPolicyRows();
        bindActions();
        try {
            getContentResolver().registerContentObserver(
                    BypassSettingsProvider.CONTENT_URI, false, settingsObserver
            );
            observingSettings = true;
        } catch (RuntimeException ignored) {
            observingSettings = false;
        }
        refresh();
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.root_layout);
        root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            view.setPadding(
                    windowInsets.getSystemWindowInsetLeft(),
                    windowInsets.getSystemWindowInsetTop(),
                    windowInsets.getSystemWindowInsetRight(),
                    windowInsets.getSystemWindowInsetBottom()
            );
            return windowInsets;
        });
        root.requestApplyInsets();
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
        if (observingSettings) {
            try {
                getContentResolver().unregisterContentObserver(settingsObserver);
            } catch (RuntimeException ignored) {
                // Activity teardown may race provider teardown.
            }
            observingSettings = false;
        }
        if (powerMonitor != null) {
            powerMonitor.close();
            powerMonitor = null;
        }
        super.onDestroy();
    }

    private void bindViews() {
        masterSwitch = findViewById(R.id.master_switch);
        powerSaveSwitch = findViewById(R.id.power_save_switch);
        powerExceptionSwitch = findViewById(R.id.power_exception_switch);
        preciseProgressSwitch = findViewById(R.id.precise_progress_switch);
        launcherIconSwitch = findViewById(R.id.launcher_icon_switch);
        copyBrowserSwitch = findViewById(R.id.copy_browser_switch);
        copyBrowserSelector = findViewById(R.id.copy_browser_selector);
        copyBrowserSelectorRow = findViewById(R.id.copy_browser_selector_row);
        selectAllSwitch = findViewById(R.id.select_all_switch);
        selectionSummary = findViewById(R.id.selection_summary);
        versionSummary = findViewById(R.id.version_summary);
        rescanSummary = findViewById(R.id.rescan_summary);
        runtimeSummary = findViewById(R.id.runtime_summary);
        preciseProgressStatus = findViewById(R.id.precise_progress_status);
        copyBrowserStatus = findViewById(R.id.copy_browser_status);
        powerExceptionRow = findViewById(R.id.power_exception_row);
        precisionSelector = findViewById(R.id.precision_selector);
        policyContainer = findViewById(R.id.policy_container);
        launcherIconController = new LauncherIconController(this);
        populateBrowserChoices();
        powerMonitor = new ExternalPowerMonitor(this);
        powerMonitor.setListener(connected -> runOnUiThread(() -> {
            if (lastConfig != null && !isFinishing()) {
                render(lastConfig);
            }
        }));
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
                if (enabled) {
                    mutateMode(OperatingMode.BYPASS);
                } else if (lastConfig != null
                        && lastConfig.getMode() == OperatingMode.BYPASS) {
                    mutateMode(OperatingMode.NORMAL);
                }
            }
        });
        powerSaveSwitch.setOnCheckedChangeListener((button, enabled) -> {
            if (!rendering) {
                if (enabled) {
                    mutateMode(OperatingMode.POWER_SAVE);
                } else if (lastConfig != null
                        && lastConfig.getMode() == OperatingMode.POWER_SAVE) {
                    mutateMode(OperatingMode.NORMAL);
                }
            }
        });
        powerExceptionSwitch.setOnCheckedChangeListener((button, enabled) -> {
            if (!rendering) {
                Bundle extras = new Bundle();
                extras.putBoolean(ConfigContract.KEY_POWER_EXCEPTION, enabled);
                mutate(ConfigContract.METHOD_SET_POWER_EXCEPTION, extras);
            }
        });
        preciseProgressSwitch.setOnCheckedChangeListener((button, enabled) -> {
            if (!rendering && lastConfig != null) {
                ProgressPrecision precision = enabled
                        ? lastConfig.getLastNonOriginalPrecision()
                        : ProgressPrecision.ORIGINAL;
                mutatePrecision(precision);
            }
        });
        precisionSelector.setOnCheckedChangeListener((group, checkedId) -> {
            if (rendering) {
                return;
            }
            if (checkedId == R.id.precision_tenths) {
                mutatePrecision(ProgressPrecision.TENTHS);
            } else if (checkedId == R.id.precision_hundredths) {
                mutatePrecision(ProgressPrecision.HUNDREDTHS);
            } else if (checkedId == R.id.precision_thousandths) {
                mutatePrecision(ProgressPrecision.THOUSANDTHS);
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
        copyBrowserSwitch.setOnCheckedChangeListener((button, enabled) -> {
            if (!rendering) {
                BrowserConfig current = readBrowserConfig();
                mutateBrowser(new BrowserConfig(enabled, current.packageName()));
            }
        });
        copyBrowserSelector.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                               int position, long id) {
                        if (rendering || position < 0 || position >= browserChoices.size()) {
                            return;
                        }
                        BrowserConfig current = readBrowserConfig();
                        mutateBrowser(new BrowserConfig(
                                current.enabled(), browserChoices.get(position).packageName()
                        ));
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {
                    }
                }
        );
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

    private void mutateMode(OperatingMode mode) {
        Bundle extras = new Bundle();
        extras.putString(ConfigContract.KEY_MODE, mode.name());
        mutate(ConfigContract.METHOD_SET_MODE, extras);
    }

    private void mutatePrecision(ProgressPrecision precision) {
        Bundle extras = new Bundle();
        extras.putString(ConfigContract.KEY_PROGRESS_PRECISION, precision.name());
        mutate(ConfigContract.METHOD_SET_PROGRESS_PRECISION, extras);
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
        CoverageSnapshot coverageSnapshot = readCoverage(config);
        SettingsState state = new SettingsState(
                config,
                findAicrVersion(),
                coverageSnapshot.policies(),
                coverageSnapshot.execution(),
                powerMonitor != null && powerMonitor.isConnected(),
                coverageSnapshot.preciseProgress(),
                coverageSnapshot.preciseProgressCount(),
                coverageSnapshot.preciseProgressExpected()
        );
        lastConfig = config;
        rendering = true;
        masterSwitch.setChecked(state.isBypassEnabled());
        powerSaveSwitch.setChecked(state.isPowerSaveEnabled());
        powerSaveSwitch.setEnabled(state.isPowerSaveToggleEnabled());
        powerExceptionSwitch.setChecked(state.isPowerExceptionEnabled());
        powerExceptionSwitch.setEnabled(state.isPowerExceptionEditable());
        powerExceptionRow.setAlpha(state.isPowerExceptionEditable() ? 1.0f : 0.55f);
        runtimeSummary.setText(state.runtimeSummary());
        preciseProgressSwitch.setChecked(state.isPreciseProgressEnabled());
        preciseProgressSwitch.setEnabled(state.isPreciseProgressToggleEnabled());
        preciseProgressStatus.setText(state.preciseProgressStatus());
        preciseProgressStatus.setTextColor(coverageColor(state.preciseProgressLayer()));
        setPrecisionSelection(state.selectedPrecision());
        setPrecisionSelectorEnabled(state.isPrecisionSelectorEnabled());
        launcherIconSwitch.setChecked(launcherIconController.isVisible());
        renderBrowserConfig(readBrowserConfig(), coverageSnapshot.browserCoverage(),
                coverageSnapshot.browserHookCount(), coverageSnapshot.browserHookExpected());
        selectAllSwitch.setChecked(state.isSelectAllMode());
        selectionSummary.setText(state.selectionSummary());
        versionSummary.setText(state.versionSummary());
        rescanSummary.setText(state.rescanSummary());
        for (Policy policy : Policy.values()) {
            policySwitches.get(policy).setChecked(state.isPolicySelected(policy));
            policySwitches.get(policy).setEnabled(state.isPolicyEditable(policy));
            coverageViews.get(policy).setText(state.coverageLabel(policy));
            coverageViews.get(policy).setTextColor(coverageColor(state.coverageLayer(policy)));
        }
        rendering = false;
    }

    private void populateBrowserChoices() {
        browserChoices = CopyWebsiteBrowser.queryChoices(this);
        List<String> labels = new ArrayList<>();
        for (CopyWebsiteBrowser.BrowserChoice choice : browserChoices) {
            labels.add(choice.label());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        copyBrowserSelector.setAdapter(adapter);
    }

    private BrowserConfig readBrowserConfig() {
        try {
            Bundle response = getContentResolver().call(
                    BypassSettingsProvider.CONTENT_URI,
                    ConfigContract.METHOD_GET_BROWSER_CONFIG,
                    null,
                    null
            );
            return BrowserConfigClient.decode(response);
        } catch (RuntimeException error) {
            return BrowserConfig.defaults();
        }
    }

    private void mutateBrowser(BrowserConfig config) {
        Bundle extras = BrowserConfigClient.encode(config);
        try {
            Bundle response = getContentResolver().call(
                    BypassSettingsProvider.CONTENT_URI,
                    ConfigContract.METHOD_SET_BROWSER_CONFIG,
                    null,
                    extras
            );
            rendering = true;
            renderBrowserConfig(BrowserConfigClient.decode(response),
                    BrowserHookCoverage.PENDING, 0, 0);
            rendering = false;
        } catch (RuntimeException error) {
            Toast.makeText(this, R.string.settings_write_failed, Toast.LENGTH_SHORT).show();
            refresh();
        }
    }

    private void renderBrowserConfig(
            BrowserConfig config,
            BrowserHookCoverage coverage,
            int installed,
            int expected
    ) {
        copyBrowserSwitch.setChecked(config.enabled());
        boolean hookAvailable = coverage == BrowserHookCoverage.AVAILABLE
                || coverage == BrowserHookCoverage.PARTIAL;
        copyBrowserSwitch.setEnabled(hookAvailable || config.enabled());
        boolean selectable = config.enabled() && hookAvailable && browserChoices.size() > 1;
        copyBrowserSelector.setEnabled(selectable);
        copyBrowserSelectorRow.setAlpha(selectable ? 1.0f : 0.55f);
        int selected = 0;
        for (int i = 0; i < browserChoices.size(); i++) {
            if (browserChoices.get(i).packageName().equals(config.packageName())) {
                selected = i;
                break;
            }
        }
        copyBrowserSelector.setSelection(selected, false);
        String status = switch (coverage) {
            case AVAILABLE -> "可用 · " + installed + " / " + expected;
            case PARTIAL -> "部分可用 · " + installed + " / " + expected;
            case UNAVAILABLE -> "不可用 · 当前 AICR 版本未适配";
            case PENDING -> "等待 AICR 进程上报";
        };
        copyBrowserStatus.setText(status);
        CoverageLayer layer = switch (coverage) {
            case AVAILABLE -> CoverageLayer.SEMANTIC;
            case PARTIAL -> CoverageLayer.PARTIAL;
            case UNAVAILABLE -> CoverageLayer.UNAVAILABLE;
            case PENDING -> CoverageLayer.PENDING;
        };
        copyBrowserStatus.setTextColor(coverageColor(layer));
    }

    private void setPrecisionSelection(ProgressPrecision precision) {
        int id = switch (precision) {
            case TENTHS -> R.id.precision_tenths;
            case HUNDREDTHS -> R.id.precision_hundredths;
            case THOUSANDTHS, ORIGINAL -> R.id.precision_thousandths;
        };
        precisionSelector.check(id);
    }

    private int coverageColor(CoverageLayer layer) {
        int color = switch (layer) {
            case EXACT -> R.color.status_exact;
            case SEMANTIC, FALLBACK -> R.color.status_adapted;
            case PARTIAL -> R.color.status_partial;
            case UNAVAILABLE -> R.color.status_unavailable;
            case PENDING -> R.color.status_pending;
        };
        return getColor(color);
    }

    private void setPrecisionSelectorEnabled(boolean enabled) {
        precisionSelector.setEnabled(enabled);
        for (int index = 0; index < precisionSelector.getChildCount(); index++) {
            precisionSelector.getChildAt(index).setEnabled(enabled);
        }
        precisionSelector.setAlpha(enabled ? 1.0f : 0.55f);
    }

    private void renderLauncherIconState() {
        rendering = true;
        launcherIconSwitch.setChecked(launcherIconController.isVisible());
        rendering = false;
    }

    private CoverageSnapshot readCoverage(BypassConfig config) {
        EnumMap<Policy, CoverageLayer> result = new EnumMap<>(Policy.class);
        ExecutionCoverage execution = ExecutionCoverage.PENDING;
        PreciseProgressCoverage preciseProgress = PreciseProgressCoverage.PENDING;
        int preciseProgressCount = 0;
        int preciseProgressExpected = 0;
        BrowserHookCoverage browserCoverage = BrowserHookCoverage.PENDING;
        int browserHookCount = 0;
        int browserHookExpected = 0;
        try {
            Bundle response = getContentResolver().call(
                    BypassSettingsProvider.CONTENT_URI,
                    ConfigContract.METHOD_GET_COVERAGE,
                    null,
                    null
            );
            if (response == null) {
                return new CoverageSnapshot(
                        result, execution, preciseProgress, preciseProgressCount,
                        preciseProgressExpected,
                        browserCoverage, browserHookCount, browserHookExpected
                );
            }
            String reportedKey = response.getString(
                    ConfigContract.KEY_EXECUTION_DISCOVERY_KEY
            );
            if (currentDiscoveryKey(config).stableValue().equals(reportedKey)) {
                for (Policy policy : Policy.values()) {
                    String policyValue = response.getString(ConfigContract.coverageKey(policy));
                    String policyKey = response.getString(
                            ConfigContract.coverageDiscoveryKey(policy)
                    );
                    if (policyValue != null && reportedKey.equals(policyKey)) {
                        result.put(policy, CoverageLayer.valueOf(policyValue));
                    }
                }
                String value = response.getString(ConfigContract.KEY_EXECUTION_COVERAGE);
                if (value != null) {
                    execution = ExecutionCoverage.valueOf(value);
                }
            }
            String preciseKey = response.getString(
                    ConfigContract.KEY_PRECISE_PROGRESS_DISCOVERY_KEY
            );
            if (currentDiscoveryKey(config).stableValue().equals(preciseKey)) {
                String value = response.getString(
                        ConfigContract.KEY_PRECISE_PROGRESS_COVERAGE
                );
                if (value != null) {
                    preciseProgress = PreciseProgressCoverage.valueOf(value);
                }
                preciseProgressCount = response.getInt(
                        ConfigContract.KEY_PRECISE_PROGRESS_COUNT, 0
                );
                preciseProgressExpected = response.getInt(
                        ConfigContract.KEY_PRECISE_PROGRESS_EXPECTED, 0
                );
            }
            String browserKey = response.getString(ConfigContract.KEY_BROWSER_DISCOVERY_KEY);
            if (currentDiscoveryKey(config).stableValue().equals(browserKey)) {
                String value = response.getString(ConfigContract.KEY_BROWSER_COVERAGE);
                if (value != null) {
                    browserCoverage = BrowserHookCoverage.valueOf(value);
                }
                browserHookCount = response.getInt(ConfigContract.KEY_BROWSER_HOOK_COUNT, 0);
                browserHookExpected = response.getInt(
                        ConfigContract.KEY_BROWSER_HOOK_EXPECTED, 0
                );
            }
        } catch (RuntimeException ignored) {
            // Pending is safer than disabling a row when coverage cannot be read.
        }
        return new CoverageSnapshot(
                result, execution, preciseProgress, preciseProgressCount,
                preciseProgressExpected,
                browserCoverage, browserHookCount, browserHookExpected
        );
    }

    private String findAicrVersion() {
        try {
            PackageInfo info = findAicrPackageInfo();
            return info.versionName == null ? Long.toString(info.getLongVersionCode()) : info.versionName;
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private DiscoveryKey currentDiscoveryKey(BypassConfig config) {
        try {
            PackageInfo info = findAicrPackageInfo();
            return new DiscoveryKey(
                    info.getLongVersionCode(),
                    info.lastUpdateTime,
                    ExecutionCoverageReporter.SCHEMA_REVISION,
                    config.getRescanGeneration()
            );
        } catch (Exception ignored) {
            return new DiscoveryKey(
                    0L, 0L, ExecutionCoverageReporter.SCHEMA_REVISION,
                    config.getRescanGeneration()
            );
        }
    }

    private PackageInfo findAicrPackageInfo() throws Exception {
        return getPackageManager().getPackageInfo("com.xiaomi.aicr", 0);
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

    private record CoverageSnapshot(
            EnumMap<Policy, CoverageLayer> policies,
            ExecutionCoverage execution,
            PreciseProgressCoverage preciseProgress,
            int preciseProgressCount,
            int preciseProgressExpected,
            BrowserHookCoverage browserCoverage,
            int browserHookCount,
            int browserHookExpected
    ) {
    }
}

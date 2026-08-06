# Power Save And Configurable Progress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a mutually exclusive power-saving runtime mode with an external-power exception, and make verified precise progress independently selectable at one, two, or three decimal places.

**Architecture:** Extend the existing immutable configuration snapshot and provider contract, then let `ConfigClient` combine that snapshot with a small external-power monitor for hook-time decisions. Add a dedicated execution-control hook chain for start and stop semantics while preserving the existing leaf-only bypass catalogs. Feed one persisted progress precision into the two existing precise-progress display paths.

**Tech Stack:** Java 21, Android SDK 37/minSdk 28, SharedPreferences and ContentProvider IPC, libxposed API 102, DexKit 2.2.0, JUnit 4.

**Execution note:** Work in the current worktree because its uncommitted precise-progress collector and display changes are the required baseline. Never reset, recreate, or overwrite those changes. Stage only files owned by each task.

---

## File Map

- `config/OperatingMode.java`: persisted normal, bypass, and power-saving modes plus effective-mode resolution.
- `config/ProgressPrecision.java`: original/one/two/three-decimal choices and scale.
- `config/BypassConfig.java`: immutable snapshot including mode, power exception, active precision, and last decimal precision.
- `config/ConfigContract.java`, `ConfigCodec.java`, `BundleConfigCodec.java`, `ConfigStore.java`, `BypassSettingsProvider.java`: strict IPC and persistence migration.
- `config/ConfigClient.java`: existing atomic config observer plus effective hook-time queries.
- `hook/ExternalPowerMonitor.java`: sticky battery read and dynamic power connection receiver.
- `hook/PowerSaveExecutionHooks.java`: exact and semantic `checkCanStart`/`getNeedStop` registration.
- `hook/ExecutionCoverage.java`, `hook/ExecutionCoverageReporter.java`: critical-chain result reported to the module UI.
- `hook/AicrProviderTraceHooks.java`: reject provider starts and normalize rejected UI start requests.
- Existing precise-progress classes: consume configured scale and do nothing in `ORIGINAL`.
- `ui/SettingsState.java`, `ui/MainActivity.java`, `activity_main.xml`, resources: render and mutate the new controls.

### Task 1: Immutable Configuration And Migration

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/config/OperatingMode.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/config/ProgressPrecision.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/config/BypassConfig.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/config/ConfigContract.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/config/ConfigCodec.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/config/BundleConfigCodec.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/config/ConfigStore.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/config/BypassSettingsProvider.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/config/BypassConfigTest.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/config/ConfigContractTest.java`

- [ ] Add failing tests for `NORMAL/BYPASS/POWER_SAVE`, mutual exclusion, selected-policy preservation, precision restoration after codec round-trip, and legacy `master` preference migration.
- [ ] Run `./gradlew.bat testDebugUnitTest --tests "com.wayne.hyperaicrbypass.config.*"`; expect failures for missing enums and keys.
- [ ] Add `OperatingMode.effective(boolean allowWhilePowered, boolean connected)`:

```java
public OperatingMode effective(boolean allowWhilePowered, boolean connected) {
    if (this != POWER_SAVE) return this;
    return allowWhilePowered && connected ? BYPASS : POWER_SAVE;
}
```

- [ ] Add `ProgressPrecision` with `ORIGINAL(0)`, `TENTHS(1)`, `HUNDREDTHS(2)`, and `THOUSANDTHS(3)` and reject `ORIGINAL` as `lastNonOriginalPrecision`.
- [ ] Replace the stored master boolean in `BypassConfig` with mode while keeping `isMasterEnabled()` and `withMaster(boolean)` as compatibility adapters. Add atomic mutations for mode, power exception, active precision, and last decimal precision.
- [ ] Add strict snapshot keys and provider calls `set_mode`, `set_power_exception`, and `set_progress_precision`. Teach `BundleConfigCodec` to encode enum names as strings.
- [ ] In `ConfigStore.read()`, use the new mode key when present; otherwise derive it from the old master boolean. Continue writing the old master key as a downgrade-compatible mirror.
- [ ] Run the config tests; expect all to pass.
- [ ] Commit only config and config-test files with `feat: add runtime mode configuration`.

### Task 2: Effective Runtime And External Power

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/ExternalPowerMonitor.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/config/ConfigClient.java`
- Create: `app/src/test/java/com/wayne/hyperaicrbypass/hook/ExternalPowerMonitorTest.java`
- Create: `app/src/test/java/com/wayne/hyperaicrbypass/config/ConfigClientTest.java`

- [ ] Add failing tests for USB/AC/wireless/full power being connected, unknown/disconnected being false, and the complete effective-mode matrix.
- [ ] Run the two test classes; expect failures for missing monitor and effective query methods.
- [ ] Implement `ExternalPowerMonitor` using the sticky `ACTION_BATTERY_CHANGED` intent and a process-local receiver for `ACTION_POWER_CONNECTED` and `ACTION_POWER_DISCONNECTED`. Store only an `AtomicBoolean`; unknown starts false. Expose a change listener and add `close()` to unregister.
- [ ] Extend `ConfigClient` with `effectiveMode()`, `shouldBypass(Policy)`, `shouldPause()`, and `progressPrecision()`. Existing hook callbacks must use these methods instead of evaluating `snapshot().shouldBypass(...)` directly.
- [ ] Keep configuration replacement atomic and close both observers in `ConfigClient.close()`.
- [ ] Reuse the same monitor in the settings process under `MainActivity` lifecycle so powered/unplugged summaries update without polling or provider writes.
- [ ] Run focused tests; expect all to pass.
- [ ] Commit with `feat: resolve powered runtime behavior`.

### Task 3: Critical Execution-Control Chain

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/PowerSaveExecutionHooks.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/PowerSaveHookSpec.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/HookBootstrap.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/hook/PowerSaveExecutionHooksTest.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/hook/HookDecisionTest.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/adapt/DexKitAdapterTest.java`

- [ ] Add failing tests proving that only the dedicated power-saving catalog contains `checkCanStart` and `getNeedStop`, with exact shapes `boolean(int)` and `boolean()`, while all bypass catalogs still reject every composite control method.
- [ ] Add pure callback-decision tests: pause forces start `false` and need-stop `true`; normal and bypass preserve original composite results.
- [ ] Run focused hook tests; expect failures for the missing catalog.
- [ ] Implement exact registration for `RunningStatus#checkCanStart(int)` and `RunningStatus#getNeedStop()`.
- [ ] Add DexKit fallbacks constrained by full shapes and per-method anchors: `checkCanStart error:` plus `no cloud start config`; `getNeedStop canStop:` plus `running status -> RUNNING_LEVEL_STOP(0)`.
- [ ] Register the hooks in `HookBootstrap` before normal bypass hooks. Do not hook `checkCanStop` or `setRunningStatus`.
- [ ] Run focused tests and the existing bypass catalog tests; expect all to pass.
- [ ] Commit with `feat: pause AICR at execution boundaries`.

### Task 4: Provider Start Gate And Coverage

**Files:**
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/AicrProviderTraceHooks.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/AicrProviderHookSpec.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/ExecutionCoverage.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/ExecutionCoverageReporter.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/config/CoverageStore.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/config/ConfigContract.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/config/BypassSettingsProvider.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/HookBootstrap.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/hook/AicrProviderTraceHooksTest.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/hook/ExecutionCoverageTest.java`

- [ ] Add failing pure-policy tests for `method_algo_analyse_start`, `method_algo_analyse_UNLIMITED`, unrelated provider calls, and `method_change_algo_state` with `is_run_algo=true/false`.
- [ ] Implement provider behavior: in pause mode, short-circuit the two start methods with the supplied bundle; turn a requested UI start into a paused update; never block stop, finish, progress, or unrelated calls.
- [ ] Keep exact provider registrations first, then add DexKit fallbacks constrained to `ContentProvider.call(String, String, Bundle): Bundle`. Assign anchors per entry: the database-provider entry owns `method_algo_analyse_start`, `method_algo_analyse_UNLIMITED`, and stop/finish strings; the UI-provider entry owns `method_change_algo_state`, `is_run_algo`, and paused-state strings. Never require one moved provider to contain every anchor from both entries.
- [ ] Add `PENDING/AVAILABLE/UNAVAILABLE` coverage using the existing `DiscoveryKey` (AICR version code, update time, schema revision, and rescan generation). Write `PENDING` before each discovery attempt and report `AVAILABLE` only when both `RunningStatus` hooks and both provider start branches are installed.
- [ ] Preserve a persisted `POWER_SAVE` preference across pending/unavailable coverage. Prevent new activation in UI when unavailable, but always allow an existing on state to be switched off.
- [ ] Extend the existing rescan executor so a newer generation marks the new `DiscoveryKey` pending, reruns `PowerSaveExecutionHooks` and exact/semantic provider discovery as well as normal semantic bypass discovery, then publishes the new execution coverage result.
- [ ] Run provider and coverage tests; expect all to pass.
- [ ] Commit with `feat: gate AICR provider starts`.

### Task 5: Configurable Precise Progress

**Files:**
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressDisplay.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressDisplay.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressHooks.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalPreciseProgressHooks.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/AicrProviderTraceHooks.java`
- Modify: existing progress logic, collector, snapshot, and request-policy files only where required by the current uncommitted implementation.
- Modify: existing precise/global progress unit tests.

- [ ] Extend existing tests to assert `70.9%`, `70.85%`, and `70.852%`, half-up boundaries, 0/100 clamping, and unchanged original text in `ORIGINAL`.
- [ ] Add tests proving both surfaces use the same configured scale and that `ORIGINAL` disables precise-only cache bypass and forced notifications.
- [ ] Run all progress tests; expect failures from hard-coded scale 3.
- [ ] Change both display formatters to accept `ProgressPrecision` and use its scale with `RoundingMode.HALF_UP`. Preserve current snapshot validation and percentage replacement rules.
- [ ] Gate precise-only request, payload, notification, and display work on `ConfigClient.progressPrecision() != ORIGINAL`. Do not alter bypass or power-saving decisions.
- [ ] Run all progress tests; expect all to pass without reverting current collector work.
- [ ] Commit the complete existing progress baseline plus configurable-precision changes with `feat: configure precise progress scale`.

### Task 6: Settings UI

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/drawable/progress_precision_segment.xml` and state drawables only if standard selectors are needed.
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/ui/SettingsState.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/ui/MainActivity.java`
- Modify: `app/src/test/java/com/wayne/hyperaicrbypass/ui/SettingsStateTest.java`

- [ ] Add failing state tests for mutually exclusive switches, both-off state, indented power exception enablement, powered/unplugged summaries, precision selector state, editable policies outside select-all, and pending/unavailable execution coverage.
- [ ] Add runtime-control and progress-display rows above the policy section. Use standard Android switches and a fixed-width three-option `RadioGroup` styled as a segmented selector; do not add Material Components solely for this control.
- [ ] Bind each mutation through the provider contract under the existing `rendering` guard. A mode toggle writes one mode value, so programmatic mutual exclusion cannot trigger a second mutation.
- [ ] Keep policy rows editable in every operating mode unless select-all is on or that policy's own coverage is unavailable.
- [ ] Render execution coverage honestly: allow an active unavailable power-saving mode to be turned off, refuse new activation until available, and show the powered/pause/pending/unavailable summary.
- [ ] Run UI state tests; expect all to pass.
- [ ] Commit with `feat: add power and precision controls`.

### Task 7: Regression And Device-Ready Build

**Files:**
- Modify: `README.md` only for the new controls and behavior.
- Modify: `app/build.gradle.kts` only after verification if the next release version is chosen.

- [ ] Run `./gradlew.bat testDebugUnitTest`; expect `BUILD SUCCESSFUL`.
- [ ] Run `./gradlew.bat assembleDebug`; expect a debug APK under `app/build/outputs/apk/debug/`.
- [ ] Run `git diff --check`; expect no whitespace errors.
- [ ] Review `git status --short` and confirm `.superpowers/` mockups and unrelated user files are not staged.
- [ ] If the device is online, install the debug APK with its explicit ADB serial, restart only `com.xiaomi.aicr` and the relevant gallery UI process, then validate normal, bypass, unplugged pause, powered resume, and all three precision scales. Never clear app data.
- [ ] If the device remains offline, report device validation as pending rather than claiming it passed.
- [ ] Update README with the verified behavior. Defer version/tag/release changes until device validation is complete.
- [ ] Commit verification/docs changes with `docs: explain power-saving controls`.

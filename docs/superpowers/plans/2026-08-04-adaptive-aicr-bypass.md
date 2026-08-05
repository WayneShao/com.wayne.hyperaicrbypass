# Adaptive AICR Bypass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and verify an in-place-upgradable LSPosed module with independently configurable AICR policy bypasses and version-aware semantic fallback discovery.

**Architecture:** A pure-Java policy core drives a native settings activity and policy-owned leaf hooks. An authenticated ContentProvider synchronizes immutable snapshots, rescan generations, and successful-registration coverage across processes. A per-policy orchestrator tries exact, then semantic, then scoped fallback hooks without duplicate registrations.

**Tech Stack:** Java 17, Android SDK 35, LSPosed/Xposed API 82, JUnit 4, Android instrumentation tests, ContentProvider/SharedPreferences, DexKit 2.2.0.

---

### Task 0: Reproducible Build And Upgrade Identity

**Files:**
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] Generate a Gradle 8.13 wrapper with the locally installed distribution and verify `gradlew.bat --version`.
- [ ] Record the already captured installed baseline: package `com.example.hyperaicrbypass`, `versionCode=1`, signer SHA-256 `03a937d17b2d6a88181d35e686809dc1a3640ee237a22d85a250b5e92e522292`; the same signer was verified on the existing local debug artifact.
- [ ] Set `applicationId = com.example.hyperaicrbypass`, stable authority `com.example.hyperaicrbypass.settings`, version code `2`, and version name `2.0.0` before provider work; version 2 is demonstrably higher than the captured baseline.
- [ ] Preserve the Java Xposed entry and verify a baseline debug build uses the installed module signer.

### Task 1: Policy, Snapshot, And UI State Core

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/config/Policy.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/config/BypassConfig.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/ui/SettingsState.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/config/BypassConfigTest.java`

- [ ] Write table-driven tests for all ten default-enabled policies.
- [ ] Test master-off pass-through while child selections remain stored; master-on restores them.
- [ ] Test select-all changes children but not master, and changing one row never changes another.
- [ ] Run the focused test and verify it fails because the types are missing.
- [ ] Implement the immutable model and run focused/full unit tests green.

### Task 2: Hook Specification And Independent Decisions

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/HookSpec.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/HookDecision.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/ExactHookCatalog.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/hook/HookDecisionTest.java`

- [ ] Write a table-driven test requiring exactly one owning policy, expected method shape, and neutral result for every descriptor.
- [ ] For every policy, prove disabling it restores only its original value while other enabled policies still bypass.
- [ ] Prove master-off passes all originals and master-on restores child behavior.
- [ ] Prove no catalog entry registers `checkCanStart`, `checkCanStop`, `getNeedStop`, or `setRunningStatus`.
- [ ] Verify red, implement the minimum catalog/decision core, then run focused/full tests green.

### Task 3: Discovery Key, Candidate Validation, And Coverage

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/adapt/DiscoveryKey.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/adapt/SemanticQuerySpec.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/adapt/SemanticTarget.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/adapt/CandidateValidator.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/adapt/CoverageReport.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/adapt/AdaptationModelTest.java`

- [ ] Test `configRevision` and `rescanGeneration` are independent: policy mutations increment only config revision; rescan increments only rescan generation.
- [ ] Test discovery keys include AICR version code, last update time, schema revision, and rescan generation, never config revision.
- [ ] Test every semantic query's anchors, package prefix, return type, complete parameters, static/instance expectation, and uniqueness.
- [ ] Reject zero/multiple candidates, split/classloader shape mismatch, and duplicate descriptors across policies.
- [ ] Test coverage is successful only after registration and includes policy, descriptor, process, discovery key, layer, and failure reason.
- [ ] Test aggregation ignores stale versions/generations and leaves missing reports pending.
- [ ] Verify red, implement the pure models, and run focused/full tests green.

### Task 4: Store Serialization And Caller Authorization

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/config/ConfigContract.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/config/ConfigCodec.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/config/CallerAuthorizer.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/config/ConfigContractTest.java`

- [ ] Test strict keys/types/payload limits, immutable snapshot encoding, and malformed-input rejection.
- [ ] Test module UID-only mutation/rescan and approved target UID-only snapshot/report operations using package lists.
- [ ] Verify red, implement, and run focused/full tests green.

### Task 5: Provider And Observed Client

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/config/ConfigStore.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/config/BypassSettingsProvider.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/config/ConfigClient.java`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/androidTest/java/com/wayne/hyperaicrbypass/config/ProviderClientTest.java`

- [ ] Write instrumentation tests for authorized snapshot reads, module-only mutations/rescan, target-only coverage, unknown caller rejection, and no URI grants.
- [ ] Test `notifyChange`, atomic client snapshot replacement, independent counter delivery, config-revision acknowledgement without discovery, malformed response retention, and absent-provider non-crash behavior.
- [ ] Verify red before provider/client implementation.
- [ ] Implement stable exported authority with Binder UID-to-package validation and no direct target preference writes.
- [ ] Run unit and instrumentation suites green.

### Task 6: Native Settings Activity

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/ui/MainActivity.java`
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/drawable/policy_row_background.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/ui/SettingsStateTest.java`

- [ ] Write failing summary/render-state tests for master, all/partial children, current AICR version, pending/exact/semantic/fallback/unavailable coverage, and rescan generation.
- [ ] Implement a scroll-safe native activity with stable row heights and independent switches.
- [ ] Run unit tests and Android resource/build validation.

### Task 7: Exact Leaf Hook Registration

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/HookBootstrap.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/ExactAicrHooks.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/HookRecorder.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/MainHook.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/hook/RegistrationPlannerTest.java`

- [ ] Write failing registration-planner tests for one descriptor/one policy, callback policy guards, success-after-registration coverage, and no duplicates.
- [ ] Write a failing bootstrap test proving exactly one initialization per approved target process, one observer, one engine, attached application classloader usage, repeated-attach no-op behavior, and rejection of non-target packages/processes.
- [ ] Implement exact 4.0.6 leaf hooks: temperature getter/predicates, charging, power, interactive, migration, count, duration argument, gap, and overload.
- [ ] Keep manual stop/completion/permission/model/database/crash paths unregistered and unchanged.
- [ ] Run focused/full tests and compile green.

### Task 8: DexKit Adapter, Cache, And Semantic Hooks

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/adapt/DexKitAdapter.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/adapt/DiscoveryCache.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/adapt/DexKitDiscovery.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/SemanticHooks.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/adapt/DexKitAdapterTest.java`

- [ ] Write failing adapter tests from search results to semantic targets, including ambiguity and malformed descriptors.
- [ ] Test cache acceptance only for identical discovery key, then re-resolution and shape validation against the active classloader.
- [ ] Add official `org.luckypray:dexkit:2.2.0` and implement package-restricted queries for missing policies.
- [ ] Close every bridge in `finally`; any query/resolution/registration failure reports unavailable.
- [ ] Run focused/full tests and inspect packaged DexKit native libraries.

### Task 9: Layer Orchestration, Scoped Fallback, And Live Rescan

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/HookEngine.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/FrameworkFallbackHooks.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/RescanCoordinator.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/hook/HookEngineTest.java`

- [ ] Write failing per-policy state-machine tests for exact then semantic then fallback ordering.
- [ ] Prove a successful earlier layer suppresses later layers, installed descriptors are never re-hooked, and fallback callbacks require an identified AICR worker/call site.
- [ ] Prove config revisions only refresh/acknowledge configuration, while rescans serialize, re-report successful handles under the new key, mark only missing/unavailable policies pending, preserve installed hooks, and publish the acknowledged rescan generation.
- [ ] Implement orchestration and scoped JobScheduler/WorkManager fallbacks.
- [ ] Run focused/full tests and compile green.

### Task 10: Build And Exact Artifact Gate

**Files:**
- Modify: `app/src/main/res/values/arrays.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] Add `com.xiaomi.aiservice` to recommended scope without Android System.
- [ ] Run clean unit tests, available instrumentation tests, lint, debug build, and release build.
- [ ] Hash the exact candidate APK and inspect its package ID, version code 2 (higher than captured installed version 1), signer, provider/activity, Xposed entry, and ABIs.
- [ ] Verify signer SHA-256 equals the currently installed signer captured earlier. Do not install a different artifact.

### Task 11: Deferred Device Deployment And Functional Verification

**Precondition:** The user explicitly states the phone has cooled, is dry after refrigerator exposure, and is ready for ADB. No device command runs before that confirmation.

- [ ] Re-enumerate/map the device and capture installed package ID/version/signer/UID plus LSPosed module row, enablement, complete scopes, raw board sensors, running preferences, and a pre-upgrade UI/log baseline.
- [ ] Abort unless the exact hashed candidate has matching package/signer, higher version, correct ABIs, provider/activity, and Xposed entry.
- [ ] Install that artifact using `adb -s <serial> install -r`; never uninstall.
- [ ] Compare UID, LSPosed row, enablement, and scopes before/after and restart only Gallery/AICR application processes.
- [ ] Wait for current discovery-generation acknowledgement and successful registration reports.
- [ ] From the real Gallery UI, start analysis and sample for at least 60 seconds: require no fresh `主动建库，超温`, positive state throughout, active UI, and either monotonically advancing progress or active algorithm processes.
- [ ] For temperature-off negative control, first confirm the target acknowledged the new generation and raw sensor remains above the same threshold. Require original rejection/stopped UI; otherwise mark inconclusive.
- [ ] Restore all-enabled, wait for acknowledgement, and repeat the positive check.

# Adaptive AICR Bypass Design

## Objective

Upgrade HyperAICRBypass from a fixed sensor spoofer into an in-place-upgradable LSPosed module with a native settings screen, independent policy switches, select-all behavior, live cross-process configuration, per-policy coverage reporting, and version-aware fallback discovery for updated AICR builds.

## Confirmed Failure

On AICR 4.0.6, the interactive unlimited-start path calls `RunningStatus.checkOverStartTemperatureLimit(4)`. AICR reads the Nezha board sensors directly from `/sys/class/thermal/thermal_message/board_sensor_charge_temp` and `/sys/class/thermal/thermal_message/board_sensor_temp`. The installed module only rewrites persisted `runningStatus=-2` to `0`; it does not prevent the preceding temperature predicate from returning true. This hides the cause and leaves the UI stopped.

## Product Behavior

The settings activity contains a master switch, a select-all switch, the detected AICR version, aggregate coverage, a rescan action, and one row per policy:

1. Temperature
2. Charging
3. Battery power
4. Screen and idle state
5. Device migration state
6. Daily run count
7. Maximum run duration
8. Inter-run gap
9. Joyose overload scene
10. JobScheduler and WorkManager constraints

All policies default to enabled for this upgrade. Disabling the master switch makes every callback pass through the original value. Individual switches are evaluated in the hook callback, so configuration changes do not require reinstalling hooks.

## Architecture

### Configuration Plane

`Policy` defines stable keys and labels. `BypassConfig` is an immutable snapshot used by callbacks. `ConfigStore` persists module-owned preferences. An exported, read-only-for-settings `ContentProvider` exposes configuration to AICR processes and accepts narrowly validated coverage reports. A target-process `ConfigClient` caches the snapshot and refreshes it through a `ContentObserver`.

### Hook Plane

`MainHook` initializes once from `Application.attach`. `HookEngine` installs three layers per policy:

1. Exact AICR leaf hooks for known 4.0.6 domain predicates and getters.
2. DexKit semantic discovery only for policies whose exact target was not found. Queries combine stable strings, complete method shapes, static/instance expectations, package restrictions, and return types.
3. Scoped Android API, JobScheduler, and WorkManager fallbacks only when no exact or unique semantic registration succeeded for that policy.

Every registered descriptor has exactly one owning `Policy`. Temperature hooks only temperature getters/predicates; charging, power, interactive state, migration state, and count hook their leaf getters. Duration changes only the start-time argument supplied to `RunLevel.canStop(long)`/`StatusBean.canStop(long)`, leaving all other stop inputs intact. Gap and overload use their dedicated predicates. The engine never hooks composite `checkCanStart`, `checkCanStop`, `getNeedStop`, or `setRunningStatus`. Manual stop, genuine completion, permission failures, model failures, database failures, and crashes retain their original semantics.

### Update Adaptation

The configuration carries two independent monotonic counters. Every settings mutation increments `configRevision` and requires a target acknowledgement without rediscovery. Only the rescan action increments `rescanGeneration`. The target derives a discovery key from AICR `versionCode`, `lastUpdateTime`, module discovery-schema revision, and `rescanGeneration`. Cached discovered descriptors are used only for the identical key and are re-resolved and shape-validated against the active target classloader. A changed discovery key invalidates the mapping. Exact hooks are attempted first; missing policies are scanned with DexKit. Each candidate is checked for declaring package, return type, complete parameter shape, static/instance expectation, required anchors, and uniqueness before installation. Ambiguous candidates are rejected and reported unavailable rather than hooked broadly.

Both counters are delivered through `ContentObserver`. Rescans are serialized, operate only on missing/unavailable policies, and maintain an installed-descriptor set so an Xposed method is never hooked twice. Existing successful hook handles are re-reported for the new discovery key without re-hooking; missing/unavailable coverage becomes pending until fresh registration reports arrive.

Semantic anchors include:

- `主动建库，超温`
- `getNeedStop canStop:`
- `is overloadScene:`
- `checkIsOverloadScene fail`
- `/sys/class/thermal/thermal_message/board_sensor_temp`
- `run_level`
- `runningStatus`

### Coverage Plane

Each policy reports one of `PENDING`, `EXACT`, `SEMANTIC`, `FALLBACK`, or `UNAVAILABLE`, plus matched descriptors or a failure reason. Success is recorded only after Xposed returns a successful registration handle. Reports include process, discovery key, generation, and layer. The provider aggregates only the current discovery key/generation and never treats a configured scope, a discovered candidate, or a stale process report as proof that a hook is active.

## Provider Security And Synchronization

The stable provider authority is `com.example.hyperaicrbypass.settings`. Snapshot reads are limited by Binder calling UID to the module, Gallery, AICR, and Xiaomi AI Service packages. Setting mutation and rescan are accepted only from the module UID. Coverage reports are accepted only from approved target UIDs and strict key/type/size allowlists. The provider grants no URI permissions. Target processes never write preferences directly. Missing or malformed provider responses leave an atomic cached snapshot in place and never crash the host.

Master-off preserves all child selections while making every hook pass through. Re-enabling master restores those selections. Select-all changes child selections but not the master switch. UI changes call `notifyChange`; the target replaces its immutable cached snapshot atomically and acknowledges `configRevision` in coverage without invalidating discovery.

`Application.attach` initialization is guarded once per approved target process and uses the attached application classloader. Repeated callbacks cannot install another observer, engine, discovery run, or reporter. Non-target packages and explicitly excluded processes are ignored.

## Upgrade And Safety

The APK application ID changes to the currently installed `com.example.hyperaicrbypass`. The local and installed APKs share signer SHA-256 `03a937d17b2d6a88181d35e686809dc1a3640ee237a22d85a250b5e92e522292`, allowing an in-place upgrade that preserves LSPosed enablement and scopes. Installing a new APK does not update code already loaded into AICR; verification therefore restarts only the Gallery/AICR application processes, never Android, Zygisk, `lspd`, or `system_server`.

## Verification

1. Unit tests prove defaults, master/select-all state preservation, all ten independent policy decisions, forbidden composite descriptors, layer ordering/deduplication, discovery-key invalidation, semantic candidate validation, rescan serialization, and current-generation coverage aggregation.
2. Provider/client instrumentation tests prove caller authorization, snapshot observation, atomic cache replacement, malformed-response fallback, and current-generation reporting.
3. Debug and release builds must succeed.
4. The exact hashed APK's package, higher version code, signer, manifest provider/activity, Xposed entry, and native DexKit libraries are inspected before installation.
5. In-place install must preserve package UID, LSPosed module row, enablement, and scopes.
6. Fresh LSPosed logs must prove module injection and successful hook registration.
7. Starting Gallery AI analysis must produce no fresh `主动建库，超温`, must remain positive across multiple samples, must keep the UI active, and must show advancing progress or active algorithm processes.
8. A temperature-off negative control is valid only while the raw sensor remains above the same threshold and after the target acknowledges the new config generation. Otherwise it is reported inconclusive.

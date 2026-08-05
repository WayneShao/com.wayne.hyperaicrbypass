# Global Precise AI Search Progress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Display AICR scope-31 progress with three HALF_UP decimal places in both controls of AiSearchSettingActivity while preserving the native integer bar and existing gallery-only precision.

**Architecture:** Add a separate global precision pipeline. A request-local collector observes the exact AICR branch and nested component calculations, a pure calculator replays the native integer pipeline while retaining rational precision, a distinct Bundle payload crosses direct and component-push paths, and a setting-screen hook updates both TextViews transactionally. Global and gallery hook catalogs and readiness gates remain independent.

**Tech Stack:** Java 17, Android Bundle and reflection, Xposed API 82, DexKit 2.2.0, JUnit 4, Gradle Android plugin.

---

## MVP Execution Order

Deliver the verified current-device path before the full compatibility matrix:

1. Implement the exact AICR 4.0.6 migrated/direct-AI calculation currently
   selected by the installed Gallery manifest (`useCoreAlgo=true`).
2. Add the distinct global payload, the `updateScopeUIProgressInfo` outgoing
   Bundle bridge, and coordinated setting-screen rendering.
3. Reuse the already-working Gallery scope-1 forced notification for live MVP
   updates while Gallery analysis is active.
4. Build, deploy, and verify both controls on the current device.
5. Only after live proof, add migration-postprocessed, unmigrated, contributor
   notification, and semantic fallback coverage from the remaining tasks.

The MVP still uses focused tests for formula replay, payload coexistence, and
render decisions. It does not wait for the exhaustive branch/failure matrix.

## File Structure

Create focused classes under app/src/main/java/com/wayne/hyperaicrbypass/hook/:

- GlobalProgressBranch.java: exact branch enum.
- GlobalProgressComponent.java: rational component plus native fixed result.
- GlobalProgressSnapshot.java: final thousandths, native integer, branch, run identity, generation, and age.
- GlobalProgressMath.java: pure native replay and precise calculations.
- GlobalProgressRequestCollector.java: reentrant ThreadLocal request, scope, and Gallery-call stacks.
- GlobalProgressPayload.java: independent namespaced Bundle and map codec.
- GlobalProgressDisplay.java: carrier validation and immutable render plan.
- GlobalProgressHookCatalog.java: exact signatures and semantic fallbacks.
- GlobalProgressHookLogic.java: Android-free bridge, readiness, and notification predicates.
- GlobalPreciseProgressHooks.java: Xposed registration and runtime callbacks.

Modify HookBootstrap.java only to install the independent global hook set. Do not merge the new payload, snapshots, or readiness state with the existing PreciseProgress classes.

### Task 1: Exact component and branch arithmetic

**Files:**

- Create: app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressBranch.java
- Create: app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressComponent.java
- Create: app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressSnapshot.java
- Create: app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressMath.java
- Test: app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressMathTest.java
- Test: app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressSnapshotTest.java

- [ ] **Step 1: Write failing tests**

Cover numerator=inverted+2*vector, denominator=2*original, natural 99.xxx, deliberate boundary decisions, denominator-zero completion, saturation, negative input, checked int overflow, HALF_UP formatting, local four-scope averaging, migrated two-stage truncation, migration post-processing, and unmigrated-local behavior.

- [ ] **Step 2: Run RED**

~~~powershell
.\gradlew.bat testDebugUnitTest --tests '*GlobalProgressMathTest' --tests '*GlobalProgressSnapshotTest' --no-daemon
~~~

Expected: compilation failure because the model classes do not exist.

- [ ] **Step 3: Implement minimal models and arithmetic**

Use only MIGRATED_DIRECT_AI, MIGRATED_POSTPROCESSED, and UNMIGRATED_LOCAL. Compatibility replay uses checked Java int intermediates and original float operation order. Additional precision uses long and BigDecimal. Store final display as 0 through 100000 thousandths-percent.

- [ ] **Step 4: Run GREEN and commit**

~~~powershell
.\gradlew.bat testDebugUnitTest --tests '*GlobalProgressMathTest' --tests '*GlobalProgressSnapshotTest' --no-daemon
git add app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgress*.java app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressMathTest.java app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressSnapshotTest.java
git commit -m "feat: model precise global AICR progress"
~~~

### Task 2: Reentrant same-request collection

**Files:**

- Create: app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressRequestCollector.java
- Test: app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressRequestCollectorTest.java

- [ ] **Step 1: Write failing lifecycle tests**

Cover nested request contexts, scope push/pop, three-argument and eight-argument candidates, getFixedProgress verification, final getGalleryProgress acceptance, migration post-processing, early migration returns without candidates, cached reuse, reentrant calls, native exceptions, hook exceptions, and unconditional finally cleanup.

- [ ] **Step 2: Run RED**

~~~powershell
.\gradlew.bat testDebugUnitTest --tests '*GlobalProgressRequestCollectorTest' --no-daemon
~~~

- [ ] **Step 3: Implement a ThreadLocal deque**

A request owns generation, scope stack, component map, branch, nested Gallery-call frame, and final snapshot. Lifecycle methods return tokens or require matching frame identities so one callback cannot pop another frame.

- [ ] **Step 4: Run GREEN and commit**

~~~powershell
.\gradlew.bat testDebugUnitTest --tests '*GlobalProgressRequestCollectorTest' --no-daemon
git add app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressRequestCollector.java app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressRequestCollectorTest.java
git commit -m "feat: collect global progress within one request"
~~~

### Task 3: Independent global payload

**Files:**

- Create: app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressPayload.java
- Test: app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressPayloadTest.java

- [ ] **Step 1: Write failing codec tests**

Require prefix com.wayne.hyperaicrbypass.global_precise_progress. Test map and Bundle round trips, malformed types, unsupported versions, bad ranges, bad branch, and coexistence with every existing Gallery payload key without overwrite.

- [ ] **Step 2: Run RED, implement codec, and run GREEN**

~~~powershell
.\gradlew.bat testDebugUnitTest --tests '*GlobalProgressPayloadTest' --no-daemon
~~~

Encode version, thousandths, fixed integer, branch, run start, request generation, and capture time. Decode only complete well-typed payloads.

- [ ] **Step 3: Commit**

~~~powershell
git add app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressPayload.java app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressPayloadTest.java
git commit -m "feat: add global precise progress payload"
~~~

### Task 4: Coordinated render planning

**Files:**

- Create: app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressDisplay.java
- Test: app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressDisplayTest.java

- [ ] **Step 1: Write failing validation tests**

Test carrier scope 31, positive status, native integer equality, exact branch allow-list, run identity, zero-to-six-minute age, both controls containing the same native percentage, paused labels, missing tokens, and suffix-preserving replacement.

- [ ] **Step 2: Run RED and implement immutable RenderPlan**

The plan carries all three original values and all three replacements required for rollback. It has no Android UI dependency.

- [ ] **Step 3: Run GREEN and commit**

~~~powershell
.\gradlew.bat testDebugUnitTest --tests '*GlobalProgressDisplayTest' --no-daemon
git add app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressDisplay.java app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressDisplayTest.java
git commit -m "feat: plan global progress screen rendering"
~~~

### Task 5: Adaptive hook catalog and pure runtime decisions

**Files:**

- Create: app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressHookCatalog.java
- Create: app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressHookLogic.java
- Test: app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressHookCatalogTest.java
- Test: app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressHookLogicTest.java

- [ ] **Step 1: Write failing catalog tests**

Require exact and semantic points for local and Gallery calculateProgress, getFixedProgress, calculateScopeProgress, calculateProgressOnMigrate, final getGalleryProgress, migrated and unmigrated branch methods, getIndexProgress, updateScopeUIProgressInfo, sendProgressToActivity, and refreshAISearchStatus.

- [ ] **Step 2: Write failing bridge/readiness tests**

Cover independent global readiness, direct scope-31 attachment, outer component Bundle attachment through global_analyse_progress, wrong integer, wrong run/branch, stale snapshot, contributor scopes 1/2/4/8/16, and unrelated scopes.

- [ ] **Step 3: Run RED, implement, and run GREEN**

~~~powershell
.\gradlew.bat testDebugUnitTest --tests '*GlobalProgressHookCatalogTest' --tests '*GlobalProgressHookLogicTest' --no-daemon
~~~

Follow PreciseProgressHookCatalog's exact-first and unique-semantic pattern. Keep decisions Android-free.

- [ ] **Step 4: Commit**

~~~powershell
git add app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressHookCatalog.java app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressHookLogic.java app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressHookCatalogTest.java app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressHookLogicTest.java
git commit -m "feat: define adaptive global progress hooks"
~~~

### Task 6: Runtime Xposed pipeline

**Files:**

- Create: app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalPreciseProgressHooks.java
- Modify: app/src/main/java/com/wayne/hyperaicrbypass/hook/HookBootstrap.java
- Test: app/src/test/java/com/wayne/hyperaicrbypass/hook/HookBootstrapTest.java

- [ ] **Step 1: Extend bootstrap tests first**

Assert a successful global set contributes to initialization without changing policy coverage or Gallery readiness.

- [ ] **Step 2: Implement exact-first and unique-semantic registration**

Every callback checks AI_UI_CAPABILITY, catches all failures, and logs one concise line. Missing global points set only globalChainReady=false.

- [ ] **Step 3: Wire request and calculation callbacks**

Start the collector only for scope 31. Wire scope stack, calculators, fixed-progress verification, migration post-processing, final Gallery return, and actual branch return. Publish only a complete snapshot to an AtomicReference.

- [ ] **Step 4: Wire both transport paths**

Attach to a direct scope-31 result. Before updateScopeUIProgressInfo, attach to the outgoing outer Bundle only when global_analyse_progress or direct analyse_progress matches a fresh same-run snapshot. Never remove or rewrite Gallery keys.

- [ ] **Step 5: Wire independent notifications**

Before sendProgressToActivity, set only forceUpdate for contributor scopes when the global chain and freshness rules pass. Existing PreciseProgressHooks independently handles Gallery scope 1.

- [ ] **Step 6: Wire transactional UI application**

After refreshAISearchStatus, preflight tvAiSearchDesc, mbtAnalyze, and mbtAnalyze.mTextView. Build one RenderPlan, update both TextViews and content description, and restore all originals after any failure. Do not call updateProgressText or alter the bar.

- [ ] **Step 7: Run full tests and build**

~~~powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug --no-daemon
~~~

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

~~~powershell
git add app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalPreciseProgressHooks.java app/src/main/java/com/wayne/hyperaicrbypass/hook/HookBootstrap.java app/src/test/java/com/wayne/hyperaicrbypass/hook/HookBootstrapTest.java
git commit -m "feat: hook global precise AICR progress"
~~~

### Task 7: Deploy and verify the real screen

- [ ] **Step 1: Reconfirm exact device and installed AICR**

~~~powershell
adb devices -l
adb -s 192.168.8.120:5555 shell dumpsys package com.xiaomi.aicr
~~~

Require device nezha and AICR 4.0.6. Do not alter LSPosed scope or enablement.

- [ ] **Step 2: Install and restart only scoped applications**

~~~powershell
adb -s 192.168.8.120:5555 install -r app\build\outputs\apk\debug\app-debug.apk
~~~

Use only the previously authorized force-stop and relaunch of com.xiaomi.aicr and com.miui.gallery. Do not reboot, restart LSPosed or system_server, clear data/cache, or delete anything.

- [ ] **Step 3: Verify hooks and branch evidence**

Require all current exact global points, MIGRATED branch evidence, final-Gallery acceptance, direct or outgoing bridge attachment, and no callback exception. A partial global chain must leave Gallery precision live.

- [ ] **Step 4: Verify both current-screen controls**

Dump UI XML and screenshot. Require the description and button to show the same NN.NNN% while the native bar stays integer. Keep the Activity open and prove a same-integer fractional update without recreating it.

- [ ] **Step 5: Verify health**

Confirm AICR still processes, no new crash/ANR/Xposed error exists, pause/resume remains native, and Gallery progress retains independent precision.

### Task 8: Final verification and review

- [ ] **Step 1: Run fresh verification**

~~~powershell
.\gradlew.bat clean testDebugUnitTest assembleDebug --no-daemon
git diff --check 8e2d57a..HEAD
git status --short
git log --oneline --decorate -15
~~~

- [ ] **Step 2: Request focused code review**

Review native replay, collector cleanup, final-Gallery boundary, payload coexistence, outgoing bridge, independent readiness, and UI rollback. Resolve every Critical and Important finding with tests.

- [ ] **Step 3: Prepare the stable branch**

Do not merge or push until live verification passes and the user chooses the integration action.

# Precise AI Search Progress Display Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Display Xiaomi AICR gallery analysis progress as a half-up rounded percentage with exactly three fractional digits while leaving AICR's scan, integer progress, and progress bar unchanged.

**Architecture:** Capture AICR's eight calculator arguments in the calculation process, attach a versioned module-namespaced payload to AICR's existing `ProgressMonitor.getIndexProgress` Bundle, and decode that payload in the progress Activity process. Exact current signatures are attempted first; each missing point gets a strict, unique-candidate DexKit fallback, and every failure leaves AICR's original integer label untouched.

**Tech Stack:** Java 17, Android `Bundle`/Binder, LSPosed Xposed API 82, DexKit 2.2.0, JUnit 4, Gradle Android plugin 8.7.3

---

### Task 1: Exact progress model and text rendering

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressSnapshot.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressDisplay.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressSnapshotTest.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressDisplayTest.java`

- [ ] **Step 1: Write failing snapshot tests**

Cover all eight inputs, long multiplication/addition beyond `int`, invalid zero denominator, denominator-less result `100`, exact integer compatibility, negative/future age, and the inclusive six-minute age boundary. The core assertions are:

```java
PreciseProgressSnapshot snapshot = PreciseProgressSnapshot.create(
        100_000, 95_999, 83_754, 83_939, 83_953,
        83_173, 0, 83_895, 70, 1_000L
).orElseThrow();
assertEquals(418_714L, snapshot.numerator());
assertEquals(595_999L, snapshot.denominator());
assertTrue(snapshot.isCompatible(70, 361_000L));

assertEquals(10_100_000_000L, PreciseProgressSnapshot.create(
        2_000_000_000, 100_000_000, 1, 1, 1, 1, 1, 1, 1, 0L
).orElseThrow().denominator());
```

- [ ] **Step 2: Run the snapshot tests and verify RED**

Run:

```powershell
$env:JAVA_HOME='D:\Backup\Desktop\tools\jdk17_20260316\jdk-17.0.18+8'
.\gradlew.bat :app:testDebugUnitTest --tests '*PreciseProgressSnapshotTest' --no-daemon
```

Expected: FAIL because `PreciseProgressSnapshot` does not exist.

- [ ] **Step 3: Implement the immutable snapshot**

Implement a record with `numerator`, `denominator`, `fixedProgress`, and `capturedElapsedRealtime`. Its factory performs all arithmetic as `long`, returns a special denominator-less snapshot only when AICR returned `100`, and otherwise rejects a non-positive denominator. Add:

```java
public boolean isCompatible(int uiProgress, long nowElapsedRealtime) {
    long age = nowElapsedRealtime - capturedElapsedRealtime;
    return fixedProgress == uiProgress && age >= 0L && age <= 360_000L;
}
```

- [ ] **Step 4: Write failing formatting and replacement tests**

Cover `70.25449 -> 70.254%`, `70.25450 -> 70.255%`, `0.000%`, `100.000%`, the special denominator-less AICR completion, clamping above `100`, first-token-only ASCII replacement, no-token passthrough, wrong scope, stale snapshot, and integer mismatch. Include:

```java
assertEquals("70.254%", PreciseProgressDisplay.format(snapshot(7_025_449, 10_000_000)));
assertEquals("70.255%", PreciseProgressDisplay.format(snapshot(7_025_450, 10_000_000)));
assertEquals("100.000%", PreciseProgressDisplay.format(completedSnapshot()));
assertEquals("已完成 70.254%，预计稍后完成",
        PreciseProgressDisplay.render("已完成 70%，预计稍后完成",
                "com.miui.gallery", 70, snapshot, 2_000L));
assertEquals("70.254% / 70%",
        PreciseProgressDisplay.replaceFirstPercentage("70% / 70%", "70.254%"));
```

- [ ] **Step 5: Run the display tests and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests '*PreciseProgressDisplayTest' --no-daemon
```

Expected: FAIL because `PreciseProgressDisplay` does not exist.

- [ ] **Step 6: Implement decimal formatting and fail-open rendering**

Use `BigDecimal`, `RoundingMode.HALF_UP`, and `[0-9]{1,3}(?:\.[0-9]+)?%`. Return `100.000%` before division for the valid denominator-less completion snapshot; otherwise divide directly to scale three, clamp to `0.000..100.000`, call `toPlainString()`, and replace only `Matcher.start()/end()` for the first token. `render` returns the original string unless the scope is exactly `com.miui.gallery` and the snapshot is compatible.

- [ ] **Step 7: Run focused tests and commit**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests '*PreciseProgressSnapshotTest' --tests '*PreciseProgressDisplayTest' --no-daemon
git add app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressSnapshot.java app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressDisplay.java app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressSnapshotTest.java app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressDisplayTest.java
git commit -m "feat: model precise gallery progress"
```

Expected: focused tests PASS and the commit succeeds.

### Task 2: Versioned cross-process payload

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressPayload.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressPayloadTest.java`

- [ ] **Step 1: Write failing payload codec tests**

Test pure `Map<String, Long>` encoding/decoding so JVM tests do not depend on Android's stubbed `Bundle`. Verify namespaced keys, round trip, missing key rejection, wrong version rejection, wrong value type rejection, and invalid reconstructed snapshot rejection.

```java
Map<String, Long> payload = PreciseProgressPayload.encode(snapshot);
assertTrue(payload.keySet().stream().allMatch(
        key -> key.startsWith("com.wayne.hyperaicrbypass.precise_progress.")));
assertEquals(snapshot, PreciseProgressPayload.decode(payload).orElseThrow());
```

- [ ] **Step 2: Run the codec test and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests '*PreciseProgressPayloadTest' --no-daemon
```

Expected: FAIL because `PreciseProgressPayload` does not exist.

- [ ] **Step 3: Implement the payload codec and Bundle adapter**

Use five keys: schema version, numerator, denominator, fixed progress, and monotonic capture time. `encode/decode` owns validation; `writeToBundle` and `readFromBundle` only adapt those values to Android `Bundle`. Never read or write `analyse_progress`, `analyse_status`, or any other AICR-owned key. Catch malformed parcel values in `readFromBundle` and return `Optional.empty()`.

- [ ] **Step 4: Run focused tests and commit**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests '*PreciseProgressPayloadTest' --no-daemon
git add app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressPayload.java app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressPayloadTest.java
git commit -m "feat: add precise progress binder payload"
```

Expected: payload tests PASS and the commit succeeds.

### Task 3: Exact and adaptive Xposed hook chain

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressHookCatalog.java`
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressHooks.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/HookBootstrap.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressHookCatalogTest.java`
- Test: `app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressHooksTest.java`

- [ ] **Step 1: Write failing catalog and callback-decision tests**

Assert all three hook shapes and anchors exactly:

```text
calculateProgress: int <- eight int, anchors progress = / base / numerator
com.xiaomi.aicr.searchpro.monitor.ProgressMonitor.getIndexProgress: Bundle <- int, boolean, Function3, anchors getIndexProgress scope: / analyse_progress / analyse_status
refreshUI: void <- Bundle, anchors analyse_progress / refreshUIStatus scope:
```

Also test pure helpers that parse eight integer arguments, require gallery scope `1` for transport, reject an absent, wrongly typed, or mismatched `analyse_progress`, and do nothing when `AI_UI_CAPABILITY` is disabled.

- [ ] **Step 2: Run hook tests and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests '*PreciseProgressHookCatalogTest' --tests '*PreciseProgressHooksTest' --no-daemon
```

Expected: FAIL because the precise hook classes do not exist.

- [ ] **Step 3: Implement the hook catalog**

Define exact current owners/methods plus a `SemanticQuerySpec` for each point. All fallbacks use package prefix `com.xiaomi.aicr`, instance-method shape validation, all listed anchors, and unique-candidate acceptance only. Keep this custom catalog separate from `SemanticHookCatalog`, because these callbacks capture, transport, and render rather than forcing a return value.

- [ ] **Step 4: Implement exact hook registration and callbacks**

`PreciseProgressHooks` owns a process-local `AtomicReference<PreciseProgressSnapshot>` and three after-callbacks:

```java
// calculator process
latest.set(PreciseProgressSnapshot.create(args..., (Integer) param.getResult(),
        SystemClock.elapsedRealtime()).orElse(null));

// ProgressMonitor Bundle assembly, gallery scope only
Bundle result = (Bundle) param.getResult();
PreciseProgressSnapshot snapshot = latest.get();
OptionalInt progress = readRequiredAicrProgress(result);
if (snapshot != null && progress.isPresent() && snapshot.isCompatible(
        progress.getAsInt(), SystemClock.elapsedRealtime())) {
    PreciseProgressPayload.writeToBundle(result, snapshot);
}

// Activity process, after original refreshUI
PreciseProgressSnapshot snapshot = PreciseProgressPayload.readFromBundle(input)
        .orElse(null);
OptionalInt progress = readRequiredAicrProgress(input);
CharSequence rendered = PreciseProgressDisplay.render(
        statusView.getText(), scopePkg, progress.orElse(-1), snapshot,
        SystemClock.elapsedRealtime());
statusView.setText(rendered);
```

Every callback first checks `configClient.snapshot().shouldBypass(Policy.AI_UI_CAPABILITY)`, validates all runtime types, and requires `Bundle.containsKey("analyse_progress")` plus an actual `Integer` value before transport or rendering. It catches its own failures and logs a concise message without throwing into AICR. Resolve `mBinding` and `tvBusinessStatus` reflectively after AICR's original method has completed; do not touch the progress bar.

- [ ] **Step 5: Implement strict DexKit fallback registration**

For each exact point that failed, query with its catalog shape and anchors, filter static/instance shape, and hook only when exactly one `MethodData` remains. Log `exact`, `semantic`, or `unavailable` per point. Never accept a preferred-name tie-break when multiple candidates remain.

- [ ] **Step 6: Wire installation into AICR bootstrap**

Construct and install `PreciseProgressHooks(context, client)` in the `com.xiaomi.aicr` branch after `ConfigClient` creation. Include its installed hook count in the existing total-failure count, but do not show a separate Toast for a partial precise-progress failure.

- [ ] **Step 7: Run focused and complete unit tests, then commit**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests '*PreciseProgress*' --tests '*HookBootstrapTest' --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
git add app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressHookCatalog.java app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressHooks.java app/src/main/java/com/wayne/hyperaicrbypass/hook/HookBootstrap.java app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressHookCatalogTest.java app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressHooksTest.java
git commit -m "feat: hook precise gallery progress display"
```

Expected: all JVM tests PASS, with at least the baseline 48 tests plus the new cases.

### Task 4: Build, deploy, and verify the real AICR UI

**Files:**
- Modify: `docs/superpowers/specs/2026-08-05-precise-progress-display-design.md`
- Verify: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 1: Commit the corrected cross-process design and this plan**

Run:

```powershell
git add docs/superpowers/specs/2026-08-05-precise-progress-display-design.md docs/superpowers/plans/2026-08-05-precise-progress-display.md
git commit -m "docs: plan precise progress implementation"
```

- [ ] **Step 2: Run clean verification build**

Run:

```powershell
$env:JAVA_HOME='D:\Backup\Desktop\tools\jdk17_20260316\jdk-17.0.18+8'
.\gradlew.bat clean testDebugUnitTest assembleDebug --no-daemon
```

Expected: `BUILD SUCCESSFUL`, all tests PASS, and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 3: Re-enumerate the exact device and capture pre-deployment state**

Run:

```powershell
adb devices -l
adb -s 192.168.8.120:5555 shell getprop ro.product.device
adb -s 192.168.8.120:5555 shell pidof com.xiaomi.aicr com.xiaomi.aicr:searchDataService com.xiaomi.aicr:searchDataService_ui com.miui.gallery
```

Expected: the explicit serial is online and device is `nezha`. If it is absent, stop deployment without mutating any device.

Then use the SDK's `aapt2 dump badging` and `apksigner verify --print-certs` on
the built APK, and compare its package/version/signer with read-only
`adb -s 192.168.8.120:5555 shell dumpsys package com.example.hyperaicrbypass`.
Confirm from current LSPosed logs or its read-only configuration that the same
installed module is enabled for both `com.xiaomi.aicr` and `com.miui.gallery`
before restarting either scoped application.

- [ ] **Step 4: Install the APK and restart only scoped applications**

Run:

```powershell
adb -s 192.168.8.120:5555 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s 192.168.8.120:5555 shell am force-stop com.miui.gallery
adb -s 192.168.8.120:5555 shell am force-stop com.xiaomi.aicr
```

Do not reboot, restart `system_server`/`lspd`, clear data/cache, or delete any app/media. Launch Gallery normally after the two scoped process stops.

- [ ] **Step 5: Verify hook registration and live rendering**

Clear only logcat's volatile buffer if needed, open Gallery's AI analysis progress UI, and inspect filtered logs for all three hook points plus payload transport/render messages. Verify on screen that the localized label contains exactly three decimals and the integer progress bar remains unchanged. Compare the displayed value with the calculator hook's logged numerator/denominator using the same half-up formula.

- [ ] **Step 6: Run a final repository audit and commit any verification-only fixes**

Run:

```powershell
git status --short
git diff --check
git log --oneline --decorate -6
```

Expected: no APK, database, decompilation output, device dump, or other research artifact is tracked. Do not push until the live result is stable and explicitly ready for publication.

# Consistent Precise Progress And Store Readiness Implementation Plan

> Execute tasks in order. Add each behavioral test first and observe it fail
> before changing production code.

**Goal:** Eliminate integer/decimal bouncing in both AICR progress screens while
preserving real three-decimal calculations, then publish a store-ready 1.1.1
release candidate.

**Architecture:** Reuse the installed SearchDataBaseProvider hook to turn only
registered scope 1/31 UI progress reads into live reads. Keep exact values in the
existing Bundle payload path, force AICR's existing notifications for continuous
fractional updates, and make global rendering field-specific. Preserve native
integer progress bars and AICR storage.

**Tech Stack:** Java 21, Android SDK 37, libxposed API 102, DexKit 2.2.0, JUnit 4,
Gradle, GitHub Actions.

---

## Task 1: Define Live UI Request Policy

**Files:**
- Create: `app/src/main/java/com/wayne/hyperaicrbypass/hook/AicrProgressRequestPolicy.java`
- Create: `app/src/test/java/com/wayne/hyperaicrbypass/hook/AicrProgressRequestPolicyTest.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/AicrProviderTraceHooks.java`

1. Add failing tests proving only `method_algo_get_progress` with
   `register_ui_listener=true`, `use_cache=true`, and scope 1 or 31 requests a
   live response. Prove missing keys, non-UI calls, other methods, and other
   scopes remain unchanged.
2. Run the focused test and confirm the missing policy fails compilation.
3. Implement a pure request policy and use it in the SearchDataBaseProvider
   before-hook to set `use_cache=false` only when AI UI capability is enabled.
4. Log `precise first response live scope=N` only when the flag changes.
5. Run the focused test and the provider hook tests.

## Task 2: Force Continuous Exact Notifications

**Files:**
- Modify: `app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressHooksTest.java`
- Modify: `app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressHookLogicTest.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressHookLogic.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressHookLogic.java`

1. Add failing tests proving complete enabled chains force dedicated scope 1 and
   branch-appropriate global contributor scopes 1/2/4/8/16/31 even before a
   previous exact snapshot exists.
2. Preserve gallery exclusion for the unmigrated branch and reject incomplete
   chains and unrelated scopes.
3. Remove the previous-snapshot prerequisite from notification decisions. This
   does not start a calculation; it only makes AICR deliver the calculation its
   existing `sendProgressToActivity` call already performs.
4. Run focused notification tests.

## Task 3: Keep Valid Precise Text Across Entry And State Changes

**Files:**
- Modify: `app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressSnapshotTest.java`
- Modify: `app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressSnapshotTest.java`
- Modify: `app/src/test/java/com/wayne/hyperaicrbypass/hook/PreciseProgressDisplayTest.java`
- Modify: `app/src/test/java/com/wayne/hyperaicrbypass/hook/GlobalProgressDisplayTest.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/PreciseProgressSnapshot.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressSnapshot.java`
- Modify: `app/src/main/java/com/wayne/hyperaicrbypass/hook/GlobalProgressDisplay.java`

1. Add failing tests proving a matching native integer remains display-compatible
   after six minutes, but negative elapsed time and native mismatch do not.
2. Add a separate global display-compatibility test allowing exactly one explicit
   start/pause/status transition to rebind a verified snapshot when the native
   integer still matches. Keep strict transport compatibility for newly generated
   payloads and reject arbitrary cross-run reuse.
3. Add paused-state tests: replace a percentage in the description while leaving
   `已暂停` button and accessibility strings unchanged.
4. Add explicit tests proving absence of an exact snapshot returns the untouched
   integer string and never creates `.000%`.
5. Implement strict transport, stable same-run display, and explicit transition
   rebinding predicates. Make global rendering produce a plan when at least one
   field contains the native percentage token.
6. Add a collector test representing a proved migration early-return completion
   as an exact denominatorless gallery component; keep incomplete non-completion
   branches rejected.
7. Run focused collector, snapshot, and display tests.

## Task 4: End-To-End Hook Tests And Full Unit Suite

**Files:**
- Modify as needed under `app/src/test/java/com/wayne/hyperaicrbypass/hook/`

1. Add/adjust catalog and hook-logic tests for the current AICR 4.0.6 signatures.
2. Verify the payload is still generated from calculator inputs and HALF_UP
   formatting remains exact.
3. Run `./gradlew testDebugUnitTest` and fix only regressions caused by this
   change.
4. Review logs emitted by unit-level hook helpers for unintentional `.000%`
   synthesis paths.

## Task 5: Store Metadata And Version 1.1.1

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `README.md`
- Create: `logo.png`
- Modify if validation requires: `SUMMARY`, `SCOPE`, `SOURCE_URL`

1. Bump versionCode to 3 and versionName to 1.1.1 before the final build and
   device validation so the tested APK is the exact release candidate.
2. Add concise compatibility, privacy, degradation, support, and precise-progress
   behavior to README. State that the module reads AICR's local counters only in
   the hooked process and does not upload media or personal data.
3. Render a square store logo from the existing launcher raster resources and
   inspect its dimensions and appearance.
4. Validate SUMMARY length, SCOPE JSON, SOURCE_URL, LICENSE, repository package
   name, and README links.
5. Set the GitHub repository description to `Hyper AICR Bypass`.

## Task 6: Build And Device Verification

**Files:** no source files unless a verified defect is found.

1. Run `./gradlew clean testDebugUnitTest assembleDebug`.
2. Install the debug/candidate APK on the explicitly enumerated arm64 device.
3. Restart only `com.xiaomi.aicr` and `com.miui.gallery` scope processes; do not
   alter LSPosed module enablement or scope switches.
4. Clear relevant LSPosed/logcat buffers only if needed for attributable logs.
5. Open both screens before and during analysis, then test pause/start. Confirm
   the first percentage is precise and no later frame returns to an integer.
6. Confirm logs contain live-first-response, payload, forced notification, and
   display events without callback failures.
7. Confirm AICR's progress bars and provider `analyse_progress` values remain
   native integers.

## Task 7: Commit, Push, Tag, And Validate Release

1. Review `git diff`, generated artifacts, and repository status.
2. Commit the implementation and release metadata.
3. Push `master`, create annotated tag `3-1.1.1`, and push the tag.
4. Monitor the Signed release workflow to completion using the configured local
   proxy when required.
5. Verify the public release contains exactly the five signed APK assets and the
   universal APK reports package `com.wayne.hyperaicrbypass`, versionCode 3, and
   versionName 1.1.1.
6. Do not create the LSPosed submission issue until this release verification is
   complete; report the exact ready-to-submit issue title and remaining manual
   review expectations.

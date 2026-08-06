# Consistent Precise Progress And Store Readiness Design

## Goal

Show AICR's real progress rounded half-up to exactly three decimal places in both
the dedicated gallery progress activity and the global AI search settings page.
The first visible progress after entering a page must already be precise, both
before analysis starts and while it is running. Native integer text must not
replace a valid precise value during state changes or long-running analysis.

Prepare the source repository for a subsequent LSPosed module repository
submission without creating the external submission issue in this change.

## Non-Goals

- Do not synthesize precision by formatting an integer as `N.000%`.
- Do not alter AICR's progress bars, database state, indexing work, or completion
  decisions.
- Do not persist a second progress database or create a new polling loop.
- Do not submit to the LSPosed repository before the release has been validated.

## Confirmed AICR 4.0.6 Behavior

The UI request sets `register_ui_listener=true` and `use_cache=true`.
`SearchDataBaseProvider` forwards that flag to
`ProgressMonitor#getIndexProgress`. The gallery and local/global calculators
return a cached integer immediately and perform real database counting later in
a coroutine. `AISearchUIProvider` also keeps an in-memory `Bundle` cache.

The current module captures exact calculator inputs only during the real
calculation. Consequently the first cached bundle has no exact payload. The
global collector is stack-scoped, so it cannot associate component calculations
that occur after the cached call returns. AICR also suppresses UI notifications
while its integer percentage remains unchanged.

## Design

### Live First Response

Add an adaptive hook for `SearchDataBaseProvider#call`. For
`method_algo_get_progress` requests that register a UI listener and target scope
1 or 31, change only `use_cache` from true to false. AICR then executes its own
existing real calculation before returning the first bundle. The precise
calculator hooks remain the source of the numerator, denominator, and aggregate
formula.

The new point uses the exact class and method first and a unique DexKit semantic
fallback anchored by `method_algo_get_progress`, `register_ui_listener`, and
`use_cache`. If the hook point cannot be resolved, precise first-frame support is
reported unavailable instead of fabricating a value.

### Continuous Updates

Force AICR's existing UI notification when the corresponding precise hook chain
is installed and the feature is enabled. The dedicated gallery path uses scope
1. The global path preserves AICR's contributor scopes 1, 2, 4, 8, and 16, plus
scope 31 when it is sent directly; the unmigrated branch excludes gallery scope
1. The notification already performs a non-cached calculation, so forcing
delivery only changes whether the newly calculated bundle is sent when the
native integer has not advanced. No timer or additional database scan is
introduced.

Attach the exact payload at the existing `getIndexProgress` and
`updateScopeUIProgressInfo` boundaries. A bundle may be displayed only when its
native integer agrees with the exact snapshot. After a valid precise frame, a
same-run status-only bundle with the same integer reuses the last verified exact
snapshot, preventing `precise -> integer -> precise` bouncing. With no verified
snapshot, the native value remains unchanged and is never turned into `.000%`.

Migration early returns that prove completion (`100`) are represented as an
exact denominatorless 100% gallery component. Other branches must still supply
calculator inputs that replay AICR's native integer; otherwise no exact global
payload is emitted.

### Display Consistency

The dedicated activity continues to replace the first percentage token in its
status text.

The global settings page replaces each percentage-bearing field independently:
description, button text, and content description. This applies to running and
paused/pre-start carrier states whenever AICR actually renders a native
percentage. Non-percentage state labels such as the paused button text remain
untouched and no longer prevent the description percentage from being precise.
Completion states that AICR hides are not rewritten.

Snapshot age alone must not cause an integer fallback while the native integer
and run still match. A run-start change may carry a verified snapshot forward
exactly once only when AICR marks the bundle as a start/pause/status transition
and the native integer is unchanged. The carried snapshot is rebound to the new
run and the next forced live response replaces it. Arbitrary cross-run reuse,
negative age, and native integer mismatches remain invalid.

### Compatibility And Failure Behavior

All new hooks follow the existing exact-first, semantic-fallback discovery
pattern. A failed optional progress hook disables only the affected precise
display path. Existing bypass policies remain independently usable. Logs identify
which first-response, calculation, transport, notification, and display points
were installed.

## Testing

Unit tests must cover:

- only registered UI progress requests for scope 1 and 31 disable the cache;
- non-UI and unrelated provider calls are unchanged;
- a missing exact payload is never formatted as `N.000%`;
- paused global UI replaces the description percentage without rewriting the
  paused button label;
- start-state run changes do not cause a precise-to-integer transition when the
  integer still matches;
- native integer mismatches remain rejected;
- notifications are forced only for enabled, fully installed dedicated or
  branch-appropriate global contributor chains;
- a proved migration completion can participate as exactly `100.000%` while
  unrepresented non-completion branches remain rejected;
- both displays always format real values with three digits and HALF_UP rounding.

On-device validation must restart only the AICR UI/search processes, enter both
screens before and during analysis, exercise pause/start, and inspect LSPosed
logs for first-response payload and display events. It must also confirm that the
progress bar and AICR database values remain native integers.

## Store Preparation

- Bump to versionCode 3 and versionName 1.1.1 after verification.
- Add a square root `logo.png` derived from the existing launcher artwork.
- Update README compatibility, privacy, failure-degradation, and support text.
- Keep `SUMMARY`, `SCOPE`, `SOURCE_URL`, and MIT `LICENSE` valid.
- Set the GitHub repository description to `Hyper AICR Bypass` before submission.
- Tag the verified commit as `3-1.1.1`; the existing workflow builds and signs
  universal, arm64-v8a, armeabi-v7a, x86, and x86_64 APK assets.
- Create `[submission] com.wayne.hyperaicrbypass` only after the public release
  assets and metadata have been checked.

# Global AI Search Progress Display Design

## Goal

Show the global AI-search progress on
`com.xiaomi.aicr.aisearch.AiSearchSettingActivity` with exactly three digits
after the decimal point. Both visible percentage labels must use the same
half-up-rounded value, for example:

```text
Description: 已完成85.317%，分析过程中设备可能会轻微发热、耗电量增加
Button:      85.317%
```

The feature must preserve AICR's original scan behavior, integer progress
Bundle fields, progress-bar value, status transitions, analytics, and stored
data. It extends the existing precise gallery-progress feature and remains
controlled by the existing `AI_UI_CAPABILITY` policy.

## Confirmed Current Logic

The setting screen is `AiSearchSettingActivity`. Its private
`refreshAISearchStatus(Bundle)` method reads `analyse_status` and the integer
`analyse_progress`, then updates both controls:

```text
mBinding.tvAiSearchDesc
mBinding.mbtAnalyze
```

The screen observes global scope `31`. On the current AICR version, the
migrated global calculation combines five domains:

```text
scope 1  = gallery
scope 2  = notes
scope 4  = messages
scope 8  = files
scope 16 = recordings
```

Each domain has an equal effective weight of 20 percent. For scopes 2, 4, 8,
and 16, AICR first derives progress from:

```text
numerator   = invertedCount + vectorCount * 2
denominator = oriCount * 2
percentage  = numerator * 100 / denominator
```

The current implementation performs integer arithmetic inside
`ProgressMonitor.calculateProgress(int, int, int)`, rounds or fixes each domain
to an integer in `getFixedProgress(...)`, truncates the four-domain local
average in `calculateScopeProgress(31, ...)`, and truncates again when the
gallery contribution is combined in `getMigratedProgress(...)`. The visible
`85%` therefore cannot be made precise by formatting the existing integer.

The existing gallery precise-progress hook already captures the gallery
numerator and denominator before its integer result is returned. That snapshot
is the scope-1 component of this design.

## Design

### Per-request collection

The transport hook around
`ProgressMonitor.getIndexProgress(int, boolean, Function3)` starts a
thread-local collection only for a non-cached global-scope calculation. The
collection exists only for the duration of that method call and is always
cleared in the after-hook, including exceptional returns.

Hook `ProgressMonitor.calculateScopeProgress(int, boolean, boolean, boolean)`
to maintain the currently executing scope in the request-local collector.
Hook the private `ProgressMonitor.calculateProgress(int, int, int)` method and
associate its three arguments with the current scope. For scopes 2, 4, 8, and
16, capture the rational numerator and denominator using `long` arithmetic.
When each `calculateScopeProgress` call returns, also capture its original
fixed integer result.

The existing gallery calculator hook contributes the gallery rational and its
fixed integer result to the same request-local collector when it executes
during that global request.

Do not retain or combine components from separate `getIndexProgress` calls.
This prevents a fast-moving scope from being paired with stale values from a
previous calculation.

### Exact global value

Build a global snapshot only when the same request collected all five domain
components and the original global method returned an integer from 0 through
100. Compute each component percentage from its rational counts with decimal
arithmetic, clamp it to 0 through 100, and apply AICR's fixed-progress boundary
behavior when the component's original integer proves that AICR deliberately
forced 0, 99, or 100.

Average the five compatible component percentages with equal 20 percent
weight. Round the final percentage to exactly three fractional digits with
`RoundingMode.HALF_UP`. Store the result as integer thousandths of one percent
in an immutable snapshot, together with AICR's original global integer and the
monotonic capture time. This representation is bounded from 0 through 100000
and avoids common-denominator overflow.

Reject the snapshot unless its value is compatible with the original global
integer path. Compatibility permits fractional information discarded by
AICR's documented intermediate truncation, but does not permit a value outside
the native integer's expected adjacent range. Completed and fixed-boundary
states are represented as `0.000%`, `99.000%`, or `100.000%` when required to
match AICR's own fix-up decision.

### Transport

Attach the global snapshot to the existing scope-31 result Bundle under a new
module-namespaced payload version. Do not replace any AICR-owned key. The
payload contains only:

```text
version
thousandths_percent
fixed_progress
captured_elapsed_realtime
```

The Bundle continues through AICR's existing Provider, Binder, cache, and
StateFlow path. No new Provider, database query, preference, file, timer, or
polling loop is introduced.

### Setting-screen display

Install an after-hook on
`AiSearchSettingActivity.refreshAISearchStatus(Bundle)`. Decode the global
snapshot from that same Bundle and cache the latest valid payload in the UI
process so that an AICR status-only Bundle cannot briefly restore integer text.
Use a cached snapshot only when it is no more than six minutes old and its
fixed integer equals the current Bundle's `analyse_progress`.

When `analyse_status` is positive and the snapshot is compatible:

1. Replace only the first ASCII percentage token in
   `mBinding.tvAiSearchDesc`, preserving the localized sentence.
2. Call `mBinding.mbtAnalyze.updateProgressText(formatted, integerProgress)`.
   The first argument becomes the precise text; the second remains AICR's
   original integer so the progress bar does not change.
3. Set the button content description to the same precise text.

When the status is paused or the original control text contains no percentage,
retain AICR's original paused label. Never create percentage text in a state
where AICR did not show one.

### Live updates

The existing notification hook forces AICR's own UI refresh only for a fresh
gallery snapshot and a fully installed precise chain. Extend its readiness
gate to include the new global capture and display points before forcing
scope-31 updates. AICR still performs its own active-UI-scope check and sends
the Bundle through its Provider. There is no independent refresh scheduler.

The gallery-only progress activity remains independently usable. Failure of a
global hook must disable only global precision; it must not disable the
existing gallery precision path.

## Compatibility

Register current exact signatures first. Add unique semantic DexKit fallbacks:

- `ProgressMonitor.calculateProgress`: float-returning, three integer
  parameters, with the `oriCount`, `invertedCount`, and `vectorCount` anchors.
- `ProgressMonitor.calculateScopeProgress`: integer-returning, parameters
  `int, boolean, boolean, boolean`, with the `scope 31 progress calculate
  begin`, `include gallery`, and `scope 31 progress` anchors.
- `AiSearchSettingActivity.refreshAISearchStatus`: void-returning, one Bundle
  parameter, with `refreshUIStatus`, `analyse_progress`, and
  `currPausedByHandle` anchors.

Accept a semantic fallback only when it resolves to one candidate with the
required modifiers and shape. Log and leave the native integer UI untouched
when a point is unavailable. Catch all hook-side reflection, payload, and
formatting errors without throwing into AICR.

## Testing

Unit tests cover:

- rational progress for notes, messages, files, and recordings;
- long arithmetic when source counts exceed safe `int` multiplication;
- five-domain equal-weight aggregation;
- half-up rounding at the fourth fractional digit;
- fixed-boundary handling at 0, 99, and 100;
- rejection of incomplete or cross-request component sets;
- rejection when the native global integer is incompatible;
- payload namespacing, versioning, malformed values, and age checks;
- replacing the description's first percentage while preserving its suffix;
- button text and content-description decisions for running versus paused
  states;
- fail-open behavior when a global hook or valid snapshot is unavailable;
- independence of the existing gallery-only precise display.

Before deployment, run the complete JVM unit-test suite and build the debug
APK. Live verification must confirm that the current setting screen shows the
same three-decimal value in both controls, keeps the original integer progress
bar, updates without recreating the Activity, and does not interrupt the scan.

## Out Of Scope

- Redefining AICR progress to include quality/IQA work that AICR omits.
- Changing scan scheduling, performance, temperature, charging, or stop rules.
- Modifying any AICR database, MediaStore record, preference, or owned Bundle
  field.
- Showing fractional progress during a paused state whose native label contains
  no percentage.

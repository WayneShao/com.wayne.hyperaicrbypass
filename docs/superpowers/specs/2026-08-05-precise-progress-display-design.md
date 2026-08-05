# Precise AI Search Progress Display Design

## Goal

Show the Xiaomi AICR gallery analysis progress with exactly three digits after
the decimal point, for example `70.254%`. The display uses standard half-up
rounding and must not change AICR's scan behavior, integer progress contract,
progress bar, analytics, or persisted data.

## Confirmed Current Logic

The current AICR APK calculates gallery progress in:

```text
com.xiaomi.aicr.searchpro.monitor.GalleryProgressMonitor
    calculateProgress(
        int totalPic,
        int totalVid,
        int faceCount,
        int ocrCount,
        int tagCount,
        int clipPicCount,
        int clipVidCount,
        int petCount)
```

Its numerator and denominator are:

```text
numerator   = faceCount + ocrCount + tagCount
            + petCount + clipPicCount + clipVidCount
denominator = totalPic * 5 + totalVid
percentage  = numerator * 100 / denominator
```

The current progress UI is updated by:

```text
com.xiaomi.aicr.aisearch.progress.AISearchProgressActivity
    refreshUI(android.os.Bundle)
```

`refreshUI` reads the integer `analyse_progress`, formats it as `<integer>%`,
and writes the localized status string to `mBinding.tvBusinessStatus`.

The calculator and progress Activity do not run in the same process. The
calculator feeds
`com.xiaomi.aicr.searchpro.monitor.ProgressMonitor.getIndexProgress(...)`, whose
result Bundle is sent through the existing `SearchDataBaseProvider` and
`AISearchUIProvider` Binder path to the Activity process. Consequently,
ordinary process-local memory cannot carry precision all the way to the UI.

`RunningStatus.sendProgressToActivity(...)` computes that Bundle while the
screen is open, but its original code calls `refresh_ui_progress` only for
selected integer or status changes. Fractional changes within the same integer
percentage therefore remain invisible until a later qualifying update or the
Activity is recreated.

## Design

### Capture

Install an after-hook on the eight-argument `calculateProgress` method. Build an
immutable progress snapshot from the original arguments and the method result.
The snapshot contains the numerator, denominator, exact percentage, original
integer result, and monotonic capture time. Store the latest valid snapshot in
process-local memory in the calculation process.

No database or MediaStore query is performed. The hook uses the exact inputs
that AICR itself selected for its current progress calculation.

### Transport

Install an after-hook on
`com.xiaomi.aicr.searchpro.monitor.ProgressMonitor.getIndexProgress(int,
boolean, Function3)`. For gallery scope `1`, attach the latest compatible
snapshot to the returned Bundle using module-namespaced keys. The hook adds only
the numerator, denominator, original integer result, monotonic capture time,
and payload version. It does not replace or mutate any AICR-owned key.

This is the process boundary already used by AICR for both requested and pushed
progress updates. Android Binder copies the namespaced fields with the existing
Bundle into the UI process, and `AISearchUIProvider` preserves unknown Bundle
fields in its cache. No module provider, file, preference, or database is used
as a second transport channel.

### UI notification

Install a before-hook on
`com.xiaomi.aicr.searchpro.monitor.RunningStatus.sendProgressToActivity(int,
boolean)`. For gallery scope `1`, once a precise snapshot has been captured in
that process, change only the existing `forceUpdate` argument to `true`.
AICR still performs its original registered-UI-scope check, obtains the current
progress Bundle, calls its own `refresh_ui_progress` Provider method, and emits
through its own `StateFlow`. This closes the notification gap without polling,
a timer, reflection into the flow, or a second data source. The original
integer and status fields remain unchanged.

### Format

Format the exact percentage with `RoundingMode.HALF_UP` and exactly three
fractional digits:

```text
70.25449 -> 70.254%
70.25450 -> 70.255%
0        -> 0.000%
100      -> 100.000%
```

Calculations must avoid integer overflow and floating-point rounding ambiguity.
Use `long` for numerator and denominator and decimal arithmetic for final
formatting.

### Display

Install an after-hook on `AISearchProgressActivity.refreshUI(Bundle)`. Only when
the activity represents the gallery scope (`mScopePkg == "com.miui.gallery"`),
decode the snapshot carried by that method's Bundle and replace the first integer
or decimal percentage token in `mBinding.tvBusinessStatus` with the formatted
precise value.

The original localized sentence is preserved. Only its percentage token changes.
The progress bar continues to receive AICR's original integer value.

A snapshot is compatible when it was captured no more than six minutes ago and
its original integer result exactly equals the `analyse_progress` value in the
UI Bundle. A stale or incompatible snapshot must not alter the label. Six
minutes covers AICR's current five-minute progress cache without allowing an
old analysis run to overwrite a later UI state.

The replaceable token uses ASCII syntax: one to three digits, optionally
followed by an ASCII period and one or more digits, followed immediately by
`%`. Only the first matching token is replaced.

## Compatibility

Register the known current class and method signatures first. This implementation
must also register semantic fallbacks through the existing DexKit adaptation
layer:

- The calculator fallback requires an integer-returning method with eight
  integer parameters and the current `progress =`, `base`, and `numerator`
  calculation string anchors.
- The transport fallback requires a Bundle-returning method with `int`,
  `boolean`, and `kotlin.jvm.functions.Function3` parameters and the
  `getIndexProgress scope:`, `analyse_progress`, and `analyse_status` string
  anchors.
- The UI fallback requires a void-returning method with one `Bundle` parameter
  and the `analyse_progress` and `refreshUIStatus scope:` string anchors.
- The UI-notification fallback requires a void-returning method with `int` and
  `boolean` parameters plus the `enter sendProgressToActivity scopes：`,
  `refresh_ui_progress`, and `no ui scope Or no current scopes,no refresh`
  anchors.

Discovery is accepted only when validation resolves a single candidate. If an
exact hook and its semantic fallback are both unavailable, retain the original
integer display and report that hook point as unavailable.

Compatibility failure is fail-open: log the unavailable hook point, do not throw
into AICR, and do not change the scan state. This feature is active only when the
existing `AI_UI_CAPABILITY` policy is enabled.

## Error Handling

- When the denominator is less than or equal to zero and AICR reports 100,
  create a completed snapshot that formats as `100.000%` without division. For
  any other AICR result, reject the snapshot.
- Clamp the precise display to the inclusive range `0.000%` to `100.000%`.
- Ignore malformed or missing UI bindings and retain the original label.
- Catch hook-side reflection and formatting failures and write a concise Xposed
  log entry without crashing the target process.
- Never open, copy, checkpoint, or modify the AICR database for this feature.

## Testing

Unit tests cover:

- numerator and denominator calculation from all eight arguments;
- long arithmetic without `int` multiplication overflow;
- half-up rounding at the fourth fractional digit;
- exactly three fractional digits at 0, normal values, and 100;
- clamping when completed counts exceed the denominator;
- replacing the first percentage token while preserving localized text;
- leaving text unchanged when no percentage token exists;
- snapshot compatibility for exact integer-result equality, the six-minute age
  limit, and gallery scope;
- namespaced payload encoding and rejection of missing, malformed, or
  unsupported payload versions;
- ASCII percentage-token matching and first-token-only replacement;
- denominator-less AICR completion formatting as `100.000%`;
- fail-open behavior when no valid snapshot is available.
- forced existing UI notification only for gallery scope after a precise
  snapshot exists.

The Android build and complete JVM unit-test suite must pass before deployment.
Live verification checks that the gallery progress label shows three decimals
while the scan remains active, continues updating within one integer percentage,
and leaves its underlying integer progress unchanged.

## Out Of Scope

- Changing scan speed, scheduling, temperature policy, or charging behavior.
- Changing any AICR-owned Provider key or the integer `analyse_progress` value.
- Changing the progress bar to a fractional rendering model.
- Adding database polling or a second progress data source.

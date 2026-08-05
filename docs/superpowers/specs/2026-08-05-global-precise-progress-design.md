# Global AI Search Progress Display Design

## Goal

Show the global AI-search progress on
`com.xiaomi.aicr.aisearch.AiSearchSettingActivity` with exactly three digits
after the decimal point. The description and button must show the same
half-up-rounded value, for example `85.317%`.

The feature must preserve AICR's scan behavior, owned Bundle fields, integer
progress contracts, progress bars, state transitions, analytics, and stored
data. It extends but does not weaken the existing gallery-only precise-progress
feature and remains controlled by `AI_UI_CAPABILITY`.

## Verified Current Code

The installed AICR APK is version 4.0.6. Its
`AiSearchSettingActivity.refreshAISearchStatus(Bundle)` reads
`analyse_progress` for UI scope `31` and writes the integer percentage to:

```text
mBinding.tvAiSearchDesc
mBinding.mbtAnalyze.mTextView
```

The installed Gallery APK declares
`com.miui.gallery.useCoreAlgo=true`. AICR's
`GalleryMigrate.isSupport()` reads that exact manifest value, so the current
device takes `ProgressMonitor.getMigratedProgress(...)`. This is verified
runtime configuration, not inferred from the visible `85%`.

The module must nevertheless model all code paths present in the current AICR
APK. It must never select a formula by inspecting the displayed integer.

## Native Calculation Model

### Local components

Scopes 2, 4, 8, and 16 represent notes, messages, files, and recordings.
`ProgressMonitor.calculateProgress(int oriCount, int invertedCount,
int vectorCount)` derives each component from:

```text
numerator   = invertedCount + vectorCount * 2
denominator = oriCount * 2
exact       = numerator * 100 / denominator
```

AICR's current implementation loses the fraction before
`getFixedProgress(int scope, float progress, int unFileCount)` applies its
boundary rules. The module retains the rational value but records the actual
float input, `unFileCount`, and returned fixed integer so it can distinguish a
natural `99.xxx%` from a deliberate 99/100 fix-up.

`calculateScopeProgress(31, ..., globalIncludeGallery=false)` obtains the four
component fixed integers and returns the truncated result of:

```text
localNative = int((scope2 + scope4 + scope8 + scope16) * 0.25)
```

The module's local precise value averages the four compatible rational
components without the fraction-losing conversions. It separately reproduces
`localNative` from their fixed integers and requires equality with AICR's
returned local integer.

### Gallery branches

When Gallery migration support is enabled and migration is complete,
`GalleryProgressMonitor.getRealProgressFromAI()` uses the existing eight-count
gallery calculator. Hook
`GalleryProgressMonitor.getGalleryProgress(boolean, Function3)`, whose return
is the final gallery integer actually consumed by `getMigratedProgress`.
The final-gallery hook creates a nested gallery-call context. The module's
existing eight-count rational becomes a candidate only when it is produced
inside that call and its fixed integer equals the final method return.

While migration is in progress, AICR may call
`calculateProgressOnMigrate(float galleryAppProgress, int aiProgress,
int migratedCount, int mediaCountBefore, int mediaCountCurr)`. Hook this final
post-processor. Reproduce its native integer result from the original arguments
and replace only the compatible `aiProgress` term with the same-request precise
AI rational when deriving a precise migrated-gallery value. Its explicit
`mediaCountCurr <= 0` return is represented only when this post-processor hook
actually executes and produces a compatible boundary candidate.

Other migration branches occur earlier in `getMigratingGalleryProgress()`:

- zero previous-media count returns 100 before the post-processor;
- zero Gallery-app progress falls back directly to the AI calculator; and
- zero current-media count returns 100 before the post-processor.

The direct-AI branch is precise only when the nested eight-count candidate is
observed and the final `getGalleryProgress` return accepts it. An early boundary
return with no nested precise candidate remains native integer text; the module
does not invent `100.000%` from the final integer alone.

At `getGalleryProgress` exit, accept only a precise candidate produced within
that exact nested call whose replayed fixed integer equals the final return.
This proves which value the global formula consumed. A cached gallery return
with no nested calculation may reuse only the latest accepted final-gallery
snapshot from the same run and branch when its integer and age are compatible.
Otherwise it fails open.

`MIGRATED_DIRECT_AI` covers both a migration-complete direct AI calculation
and the migration-in-progress fallback used when Gallery-app progress is zero;
both paths consume the same verified final AI candidate.

### Global branches

`ProgressMonitor.getIndexProgress(...)` selects the branch by calling
`GalleryMigrate.isSupport()` and then one of these exact methods:

- `getMigratedProgress(int, boolean, Function3)`
- `getUnMigratedProgress(int, boolean, Function3)`

Hook both branch methods and record which one actually executed for the current
request.

For the supported/migrated scope-31 path, reproduce AICR's two truncations:

```text
localNative  = int((s2Fixed + s4Fixed + s8Fixed + s16Fixed) * 0.25)
globalNative = int(galleryFixed * 0.2 + localNative * 0.8)
```

Require `globalNative` to equal the original branch result. The precise value
uses the matching precise gallery value and the four precise local components
with the same effective weights. It may legitimately be more than one point
above `globalNative` because the native pipeline truncates twice; no vague
"adjacent range" rule is used.

For the unsupported/unmigrated path, reproduce `getUnMigratedProgress` exactly.
In the current APK, a non-cached scope-31 call returns the four-domain local
calculation without a Gallery contribution. A cached request may use persisted
integer components that have no matching rational values. Attach precision to
a cached result only when a fresh non-cached snapshot from the same branch and
run has the same native integer. Otherwise retain AICR's integer UI.

## Same-request Collection

The `getIndexProgress` hook creates a thread-local request context before a
scope-31 calculation. The context contains a monotonically increasing request
generation, the actual branch, a stack of active `calculateScopeProgress`
scopes, component rationals, native fixed integers, and an optional final
gallery value.

Nested hooks push and pop the active scope in `finally` blocks. Reentrant
`getIndexProgress` calls use a context stack rather than overwrite an outer
request. Every exit path clears its own context, including native exceptions,
policy changes, rejected data, and hook-side failures.

Build a global snapshot only from components observed within that request.
Never complete a request by borrowing a missing component from a prior request.
The immutable snapshot uses one of these exact branch values:

```text
MIGRATED_DIRECT_AI
MIGRATED_POSTPROCESSED
UNMIGRATED_LOCAL
```

An unrepresented migration early-boundary path produces no global snapshot.
The snapshot contains:

```text
thousandths_percent (0 through 100000)
fixed_progress      (0 through 100)
branch
run_start_time
request_generation
captured_elapsed_realtime
```

Use `RunningStatus.getRunningStartTime()` as the run identity. Compatibility
replay preserves Java's original evaluation order, checked `int` arithmetic,
and IEEE-754 `float` conversions. Reject a request when an original integer
multiplication or addition overflows. Use `long` counts and `BigDecimal` only
for the additional precise value and final `RoundingMode.HALF_UP` formatting
with exactly three fractional digits.

### Boundary decisions

Model native boundaries from sufficient inputs, not only the final integer:

- A non-positive denominator accepted by AICR as completion is `100.000%`.
- Counts at or beyond the denominator are `100.000%`.
- Otherwise retain the rational value, including natural `99.xxx%` values.
- If the actual float and `unFileCount` passed to `getFixedProgress` enter its
  special 99-to-100 branch, reproduce that branch and use its deliberate fixed
  boundary value.
- Reject negative counts, overflowed native intermediates, malformed values,
  or any component whose reproduced native integer differs from the observed
  result.

## Transport

### Separate payloads

The global payload uses a distinct key prefix and schema from the existing
gallery payload. A scope-1 Bundle may carry both payloads simultaneously.
Gallery and global snapshots, readiness state, decoding, and UI caches must
never share a mutable reference or version key.

The global payload is attached to a direct scope-31 `getIndexProgress` result
when the same request produced it. Cached direct results may receive only a
fresh, same-run, same-branch latest snapshot whose fixed integer matches
`analyse_progress`.

`getMigratedProgress(scope=31, cache=true)` reads persisted aggregate integers
directly. Its precision therefore uses this whole-global cached-result rule,
not the separate cached `getGalleryProgress` final-component reuse rule.

### Outgoing live-push bridge

A component push does not preserve an inner scope-31 result Bundle.
`RunningStatus.sendProgressToActivity(componentScope, ...)` copies only the
inner result's integer into `global_analyse_progress` on the outer component
Bundle. Therefore a payload attached only to `getIndexProgress(31)` is lost.

Install a before-hook on
`ProgressMonitor.updateScopeUIProgressInfo(int, Bundle)`, which executes after
the inner global calculation and before AICR sends `refresh_ui_progress`.
Attach the latest global payload to that exact outgoing Bundle only when:

- it contains `global_analyse_progress` and the snapshot fixed value equals it;
  or it is a direct scope-31 Bundle whose `analyse_progress` equals it;
- the snapshot is from the current `run_start_time` and actual branch;
- its age is no more than six minutes; and
- the complete global capture, bridge, and display readiness set is installed.

This placement lets `AISearchUIProvider` clone the outer Bundle for scope 31
with the global payload intact. It also leaves the existing gallery payload on
the same scope-1 Bundle untouched.

No new Provider, database query, preference, file, timer, or polling loop is
introduced.

## Live Notification

Keep two independent readiness sets:

- Gallery readiness contains only the existing gallery capture, transport,
  notification, and gallery-display points.
- Global readiness contains the local/final-gallery capture points, actual
  branch hooks, global payload transport, outgoing bridge, and setting-screen
  display point.

The existing gallery decision may force a scope-1 notification whenever the
gallery readiness set and freshness checks pass, regardless of global
readiness.

The global decision may force notifications for contributing scopes 1, 2, 4,
8, and 16 only when global readiness is complete and a fresh compatible
component/run context exists. AICR still performs its active-UI-scope check,
computes the current global Bundle through its own methods, and calls its own
Provider. Failure of any global point disables only global precision and global
forcing; it cannot disable gallery precision.

## Setting-screen Display

Install an after-hook on
`AiSearchSettingActivity.refreshAISearchStatus(Bundle)`. Do not keep a module
cache for global UI payloads. Render precision only from the Bundle currently
being displayed. This avoids resurrecting a snapshot after a run transition,
progress regression, completion, policy change, or incompatible newer update.
AICR's own Bundle cache retains unknown namespaced keys on compatible paths.

Accept a payload only when the carrier has scope 31, positive
`analyse_status`, the same `analyse_progress`, current run identity, one of
`MIGRATED_DIRECT_AI`, `MIGRATED_POSTPROCESSED`, or `UNMIGRATED_LOCAL`, and an
age from zero through six minutes.

Preflight all UI objects before mutation:

```text
mBinding.tvAiSearchDesc       (TextView)
mBinding.mbtAnalyze           (ProgressButton)
mBinding.mbtAnalyze.mTextView (TextView)
```

Require both original controls to contain the same native integer percentage.
Precompute the replacement description and button text. Then update the two
TextViews and the button content description without calling
`updateProgressText`; this leaves the existing progress animation and integer
bar value untouched. If any write fails, restore all three original values.
Paused or non-percentage labels remain native.

## Compatibility

Register current exact signatures first and add unique semantic DexKit
fallbacks for:

- both three-argument local `calculateProgress` and eight-argument gallery
  `calculateProgress` methods;
- `getFixedProgress(int, float, int)`;
- `calculateScopeProgress(int, boolean, boolean, boolean)`;
- `calculateProgressOnMigrate(float, int, int, int, int)`;
- `GalleryProgressMonitor.getGalleryProgress(boolean, Function3)` as the
  final-gallery consumer boundary;
- `getMigratedProgress(int, boolean, Function3)` and
  `getUnMigratedProgress(int, boolean, Function3)`;
- `getIndexProgress(int, boolean, Function3)`;
- `updateScopeUIProgressInfo(int, Bundle)`;
- `sendProgressToActivity(int, boolean)`; and
- `AiSearchSettingActivity.refreshAISearchStatus(Bundle)`.

Each semantic match validates declaring package, static/private/public
modifiers, return type, parameter types, and version-specific string anchors.
Accept only one candidate. Missing or ambiguous global points log a concise
failure and leave global UI native without affecting the gallery readiness set.
No hook exception may escape into AICR.

## Testing

Unit tests cover:

- rational component arithmetic and `long` overflow boundaries;
- all `getFixedProgress` branches, including natural `99.xxx`, deliberate 99,
  deliberate 100, denominator-zero completion, and count saturation;
- nested scope push/pop, reentrant requests, exceptions, and unconditional
  thread-local cleanup;
- full migration, migration-in-progress post-processing, zero Gallery progress
  with a direct-AI candidate, early migration boundaries without a candidate,
  and unsupported/unmigrated formulas;
- final-gallery acceptance only for a candidate produced inside the same
  `getGalleryProgress` call, including cached final-gallery reuse rules;
- deterministic reproduction of the local and global native truncations;
- rejection of incomplete, cross-request, wrong-run, wrong-branch, malformed,
  stale, or native-incompatible snapshots;
- cached-request acceptance only for a matching fresh non-cached snapshot;
- distinct global/gallery payload namespaces and coexistence on one Bundle;
- loss of the inner global Bundle and successful attachment at the outgoing
  component-Bundle bridge;
- independent gallery/global readiness failure matrices and contributor-scope
  notification decisions;
- carrier-only UI acceptance across start, pause, regression, completion, and
  policy-disable transitions;
- preflight rejection and rollback after each possible partial UI write; and
- preservation of the original integer progress bar and gallery-only display.

Before deployment, run the complete JVM suite and build the debug APK. Live
verification must use the already-open setting Activity and confirm that both
texts show the same three-decimal value while a component changes within one
integer point. It must also cover pause/resume and a scope-1 push whose one
outer Bundle serves both the gallery and global UIs.

## Out Of Scope

- Redefining progress to include quality/IQA work omitted by AICR.
- Changing scan scheduling, performance, temperature, charging, or stop rules.
- Modifying an AICR database, MediaStore record, preference, or owned Bundle
  field.
- Inventing precision from an integer-only cached state.

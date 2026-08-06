# Power Save And Configurable Progress Design

## Goal

Add a user-controlled power-saving mode that pauses AICR analysis on battery,
optionally resumes it while external power is connected, and never kills the
gallery or AICR process. Make precise progress display independent from bypass
behavior and selectable at one, two, or three decimal places.

Prefer the smallest reliable implementation. Dependency count is not a design
constraint: use an established dependency when it reduces total complexity or
compatibility risk, and use platform APIs when they are already simpler.

## Confirmed Behavior

- The existing global bypass and the new power-saving mode are mutually
  exclusive, but both may be off.
- Enabling power saving while AICR is running pauses the current work and keeps
  its indexing progress. It also blocks new starts and resumes.
- `Allow while externally powered` is a child option of power saving. External
  power means USB, AC, or wireless power is connected, including when the
  battery is full or charging is temporarily suspended.
- When that child option is enabled and power is connected, AICR runs with the
  currently selected bypass policies. Disconnecting power pauses it again.
- Precise progress is independent of both operating modes. When disabled, AICR
  keeps its original text. When enabled, the user chooses one, two, or three
  decimal places, rounded half-up.

## Configuration Model

Represent the two top-level runtime switches with one persisted `OperatingMode`
value:

- `NORMAL`: do not bypass constraints and do not force AICR to pause.
- `BYPASS`: apply the selected bypass policies.
- `POWER_SAVE`: pause AICR unless the external-power exception is active.

The UI maps `BYPASS` and `POWER_SAVE` to two switches. Changing either switch
writes one atomic mode value, which makes the impossible both-on state
unrepresentable. Selected policies remain persisted and editable in every mode;
`select all` alone controls whether individual policy rows are editable.

Persist `allowWhileExternallyPowered` separately. Model progress display with a
persisted active `ProgressPrecision` value and a persisted
`lastNonOriginalPrecision` value. Active precision is one of:

- `ORIGINAL`
- `TENTHS`
- `HUNDREDTHS`
- `THOUSANDTHS`

The precise-progress switch maps `ORIGINAL` to off and every other value to on.
Selecting a decimal scale writes both precision fields. Turning the switch off
writes only active precision as `ORIGINAL`; turning it on restores the persisted
`lastNonOriginalPrecision`. The last precision therefore survives process and
device restarts.

Existing settings migrate without data loss: `master=true` becomes `BYPASS`,
`master=false` becomes `NORMAL`, all policy selections remain unchanged,
progress defaults to `THOUSANDTHS`, and both power-saving options default off.

## Effective Runtime Behavior

Use a small pure decision function shared by hook callbacks:

| Persisted mode | External power exception | Power connected | Effective behavior |
| --- | --- | --- | --- |
| `NORMAL` | any | any | Original AICR behavior |
| `BYPASS` | any | any | Apply selected bypass policies |
| `POWER_SAVE` | off | any | Pause and block execution |
| `POWER_SAVE` | on | no or unknown | Pause and block execution |
| `POWER_SAVE` | on | yes | Apply selected bypass policies |

The function returns semantic decisions such as `ORIGINAL`, `BYPASS`, and
`PAUSE`; it does not force every boolean hook to `false`. Existing hook points
have different meanings, so each callback translates the semantic decision to
the correct return value or argument mutation.

External power state is read from the sticky battery intent at startup and kept
current with process-local power connection broadcasts. Unknown state is
treated as not externally powered. Configuration and power snapshots are
atomic so a hook never observes a partially updated state.

## Execution Control Hooks

Add a focused execution-control catalog with the following AICR 4.0.6 points,
proved by the current decompilation and logs:

- `com.xiaomi.aicr.searchpro.monitor.RunningStatus#checkCanStart(int): boolean`
  is called twice by `AISearchDataTask#doRemoteWork` before scheduled work
  starts. In effective `PAUSE`, return `false`.
- `RunningStatus#getNeedStop(): boolean` is polled by gallery, collection, and
  migration loops. In effective `PAUSE`, return `true`.
- `com.xiaomi.aicr.aisearch.provider.SearchDataBaseProvider#call(String,
  String, Bundle): Bundle` receives `method_algo_analyse_start` and
  `method_algo_analyse_UNLIMITED`. In effective `PAUSE`, return the supplied
  bundle without executing either start branch.
- `com.xiaomi.aicr.aisearch.AISearchUIProvider#call(String, String, Bundle):
  Bundle` receives `method_change_algo_state` with `is_run_algo=true`. When a
  start is rejected, change that flag to `false` before the original UI-state
  update so the page remains visibly paused.

The critical execution chain consists of both `RunningStatus` methods and the
two provider start branches. The UI-state correction is optional for execution
safety but required for a complete visual result. The existing policy bypass
catalogs continue to exclude composite `checkCanStart`, `checkCanStop`,
`getNeedStop`, and `setRunningStatus`; only this dedicated power-saving catalog
may hook `checkCanStart` and `getNeedStop`, and only while effective behavior is
`PAUSE`. It never hooks `checkCanStop` or `setRunningStatus`.

For updated AICR versions, exact signatures are attempted first. Semantic
fallback candidates must keep the full method shapes above. `checkCanStart`
uses the anchors `checkCanStart error:` and `no cloud start config`;
`getNeedStop` uses `getNeedStop canStop:` and
`running status -> RUNNING_LEVEL_STOP(0)`. Provider fallbacks require the
Android `ContentProvider.call` signature plus the start, unlimited-start, stop,
and UI-state method strings. A candidate is rejected if its return type,
parameters, or anchor set is incomplete.

In effective `PAUSE` behavior:

- reject new analysis starts;
- reject resume attempts;
- make the next running-state check request a pause;
- report AICR as unavailable to the gallery only where that result prevents an
  execution request, without hiding unrelated progress UI state;
- preserve database, index, queue, and task metadata.

Do not force-stop or delete either app, restart Android, clear data, or add a
polling loop. A power or configuration transition updates the in-process state
immediately; active work pauses at AICR's next confirmed execution boundary.

Execution coverage is keyed by AICR version code, package update time,
discovery schema revision, and rescan generation. Coverage is `PENDING` while a
new key is being checked, `AVAILABLE` only when the complete critical chain is
installed, and `UNAVAILABLE` after discovery finishes without that chain.
Optional UI correction may degrade independently.

`PENDING` or `UNAVAILABLE` never rewrites the persisted user preference. If
power saving was already selected, its switch remains visibly on and can always
be turned off, but the summary explicitly says that protection is pending or
unavailable and does not claim AICR is paused. If power saving is not already
selected, the UI refuses a new activation until coverage is `AVAILABLE`.
Becoming `AVAILABLE` later activates the still-persisted preference without the
user reselecting it. Runtime hooks that did install continue to honor `PAUSE`
while coverage is incomplete, but partial behavior is never presented as a
guarantee.

## Progress Display

Extend the existing precise-progress implementation rather than adding a second
collector. Both the dedicated gallery activity and global AI search settings
read the same `ProgressPrecision` snapshot.

When precision is not `ORIGINAL`, format verified exact progress with the
selected scale and `RoundingMode.HALF_UP`. Replace only a percentage token that
matches the native integer and current run. Retain the existing freshness,
run-identity, and denominator validation so no integer is fabricated into a
decimal and no old task snapshot is reused.

When precision is `ORIGINAL`, do not force extra UI notifications, disable
cache, attach display payloads, or rewrite text solely for precise display.
Bypass and power-saving hooks remain independently active.

## UI

The settings page contains three unframed sections:

1. Runtime control: `Global bypass` and `Power-saving disable` at the same
   level, followed by the indented `Allow while externally powered` child row.
2. Progress display: `Precise progress` followed by a one/two/three-decimal
   segmented selector. The selector is disabled while precise progress is off.
3. Bypass policies: `Select all` plus individual policy switches. Policies
   remain configurable for powered power-saving mode even when global bypass is
   off.

While powered power-saving mode is active, the summary says that external power
currently permits analysis. When unplugged, it says AICR is paused to save
power. Turning on either top-level mode automatically turns the other off.

## Failure Handling

- A malformed or incomplete configuration response leaves the last complete
  snapshot active.
- An unavailable power signal is treated as battery operation.
- A failed optional hook disables or marks only the affected feature row.
- Pending or unavailable critical execution coverage preserves the requested
  power-saving preference, prevents new activation, keeps the off action
  available, and presents no false guarantee.
- The existing total-failure toast remains reserved for cases where all useful
  hook coverage is unavailable.
- Logs identify exact versus semantic execution-control coverage and effective
  mode transitions without logging personal data or image metadata.

## Testing

Unit tests cover:

- all operating-mode, external-power, and exception combinations;
- mutual exclusion and the valid both-off UI state;
- upgrade migration from the existing master boolean;
- immediate semantic transition to pause on mode enable or power disconnect;
- powered power-saving mode applying selected bypass policies;
- preservation of policy selections across mode changes;
- original, one-, two-, and three-decimal progress behavior;
- restoration of the last non-original precision after process restart;
- half-up rounding boundaries and 0%/100% clamping;
- both progress surfaces reading the same precision;
- malformed configuration and unknown-power fallbacks;
- critical versus optional hook coverage in UI availability;
- pending, unavailable, available-after-rescan, and coverage-key changes without
  silently rewriting the persisted operating mode;
- the power-saving catalog alone may use `checkCanStart` and `getNeedStop`, while
  all bypass-policy catalogs still reject composite control methods.

On-device validation verifies start, active analysis, pause, reconnect, unplug,
and both progress surfaces. It restarts only the affected app processes when
needed for newly installed hooks and never clears application data.

## Out Of Scope

- Scheduling analysis for a future wall-clock time.
- Battery percentage or temperature thresholds inside power-saving mode.
- Killing AICR or gallery processes as a normal control mechanism.
- A second progress database, timer, or synthetic progress calculation.
- Unrelated UI redesign or refactoring outside the touched configuration,
  runtime decision, execution-control, and progress-display paths.

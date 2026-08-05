# README and Release Design

## Goal

Prepare HyperAICRBypass for public distribution and a later submission to the
Xposed Modules Repository. The repository must explain why the module exists,
what it changes, how it adapts to AICR updates, and the risks of bypassing
device-state policies. A tag must produce tested, officially signed APK assets
without committing signing material.

## Public Documentation

`README.md` is Chinese-first with a short English summary. It covers:

- the project origin: Xiaomi Gallery local AI processing would not start under
  opaque and overly strict device-state conditions, so the owner chose an
  LSPosed module as a last resort;
- the eleven independently configurable policy groups, master/select-all
  behavior, launcher-icon visibility, adaptive discovery, and three-decimal
  progress display;
- exact recommended LSPosed scopes and the scoped-process restart needed after
  installing or updating;
- the current live verification baseline (`nezha`, Android 16, HyperOS
  `OS3.0.307.6.WPACNXM`, arm64-v8a, AICR 4.0.6, Gallery
  `5.0.7.7-0720-R`, AI Service `3.12.2_dd2be79_260427_cn`), without treating
  it as a minimum-version or future-version compatibility promise;
- the narrow privacy fact that the module itself requests no Internet
  permission and adds no upload/analytics behavior, without making claims
  about network or data handling inside the hooked Xiaomi applications;
- an explicit heat, battery, stability, and data-loss warning;
- source build and release instructions.

Store metadata files `SUMMARY`, `SCOPE`, and `SOURCE_URL` mirror the official
example repository format. The package name remains
`com.example.hyperaicrbypass`; changing it would break in-place upgrades and
LSPosed module identity.

## APK Outputs

Normal developer builds remain unchanged. Passing `-PsplitAbi=true` enables
Android ABI splits and emits these release variants:

- universal;
- arm64-v8a;
- armeabi-v7a;
- x86;
- x86_64.

The universal APK is the default recommendation and the only APK intended for
the Xposed Modules Repository. Architecture-specific APKs are optional advanced
downloads from the source repository. Their ABI must match the hooked target
process ABI, which is not necessarily the same as the device's advertised ABI.
Each split contains only its matching DexKit native library. The workflow must
validate that all five exact outputs exist before signing.

## Signing

The release key is generated outside every Git worktree as a PKCS12 keystore
with owner identity `WayneShao <owner@wayneshao.com>`. Its random password is
kept in a restricted local credentials file and copied to GitHub Actions only
through encrypted repository secrets:

- `ANDROID_KEYSTORE_BASE64`;
- `ANDROID_KEY_ALIAS`;
- `ANDROID_KEYSTORE_PASSWORD`;
- `ANDROID_KEY_PASSWORD`.

No keystore, password, base64 payload, or generated APK is committed. Losing
the keystore or password makes future upgrades under the same package name
impossible, so the local signing directory must be backed up separately.

## Release Workflow

A push of any Git tag to the current source repository starts the workflow. The
workflow:

1. uses Java 17;
2. rejects missing signing secrets;
3. runs unit tests and builds all five release APKs;
4. signs and verifies every APK with Android `apksigner`;
5. reads `versionCode` and `versionName` from the signed universal APK;
6. requires the tag to equal `<versionCode>-<versionName>`;
7. rejects an already-published Release for that tag;
8. creates a draft Release with all assets and publishes it only after upload
   succeeds.

For version code `2` and version name `2.0.0`, the valid tag is `2-2.0.0`.

The source-repository Release is not automatically an Xposed Modules Repository
release. Later store submission must either transfer/rename the repository as
the official process requires, or publish the verified universal APK to the
package-named repository created by the submission bot. The official repository
release contains only the universal APK and uses the same version tag and
signing certificate.

## Failure Behavior

Missing secrets, failed tests, missing ABI outputs, signature verification
errors, or a mismatched tag fail the workflow before any public Release is
created. A failed unpublished draft may be replaced on retry. A published
Release is immutable; an APK change requires a higher application version and
new tag so store ingestion is never dependent on an asset-only edit. Existing
runtime hook fallback behavior is unchanged: exact targets are preferred,
semantic discovery requires a unique validated candidate, and an unavailable
hook does not broaden into an ambiguous hook.

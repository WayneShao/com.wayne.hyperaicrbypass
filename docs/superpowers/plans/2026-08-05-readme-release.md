# README and Signed Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add store-ready public documentation and a tag-triggered workflow that publishes signed universal and ABI-specific APKs.

**Architecture:** Keep signing outside Gradle and Git: Gradle only controls optional ABI splits, while GitHub Actions decodes an encrypted keystore, signs outputs with `apksigner`, validates APK metadata, and creates the Release. Documentation describes only behavior proven by the current source and live AICR 4.0.6 verification.

**Tech Stack:** Markdown, Android Gradle Plugin Kotlin DSL, GitHub Actions, Bash, Android build-tools `aapt2`/`apksigner`, Java 17.

---

### Task 1: Public documentation and repository metadata

**Files:**
- Create: `README.md`
- Create: `SUMMARY`
- Create: `SCOPE`
- Create: `SOURCE_URL`

- [ ] **Step 1: Write README content from current source facts**

Write Chinese-first content with a short English summary. Document the project
origin, eleven policy controls, master/select-all behavior, adaptive
exact/semantic hook behavior, precise progress display, launcher icon option,
the exact three recommended scopes, scoped-process restart requirement, narrow
privacy boundary, warnings, build commands, and signed release requirements.
Include the complete verified `nezha`/Android/HyperOS/ABI/AICR/Gallery/AI
Service baseline from the design spec.

- [ ] **Step 2: Add official repository metadata**

Use the current application ID in repository/submission instructions, use the
three manifest scope packages in `SCOPE`, and point `SOURCE_URL` to the current
GitHub repository. Clearly distinguish the five-asset source-repository Release
from the later official package-named repository Release, which receives only
the universal APK.

- [ ] **Step 3: Verify public claims**

Run `rg` against `Policy.java`, `arrays.xml`, `AndroidManifest.xml`, and the
progress hook catalogs. Expected: every README feature and scope has a matching
source fact and no Internet permission exists.

### Task 2: Opt-in ABI release outputs

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add opt-in ABI splits**

Read Gradle property `splitAbi`; when true, enable `arm64-v8a`,
`armeabi-v7a`, `x86`, `x86_64`, and universal outputs. Leave ordinary local
build behavior unchanged when the property is absent.

- [ ] **Step 2: Verify configuration statically**

Run a Gradle model/configuration task under Java 17 or let the tag workflow run
the complete build. The release workflow must derive exact APK paths and ABI
filters from `app/build/outputs/apk/release/output-metadata.json`, require one
unfiltered universal element plus exactly one element for each configured ABI,
and map those elements deterministically to final asset names. Expected:
configuration succeeds and no signing material is referenced from a repository
path.

### Task 3: Signed tag release workflow

**Files:**
- Create: `.github/workflows/release.yml`

- [ ] **Step 1: Add the failing release-contract check**

Before build/release, require all four signing secrets without echoing their
values and later require the signed universal APK tag to equal
`<versionCode>-<versionName>`. Disable shell tracing and fail non-zero on either
mismatch. Add per-tag workflow concurrency with cancellation disabled.

- [ ] **Step 2: Add build, signing, and APK verification**

Run `clean testDebugUnitTest assembleRelease -PsplitAbi=true`. Decode the
keystore only under `$RUNNER_TEMP`, set mode `600`, register an exit trap that
deletes it, and pass passwords with `apksigner`'s `env:` mechanism. Read all
five exact inputs from `output-metadata.json`, sign each, and run `apksigner
verify --verbose --print-certs` on every result. Inspect ZIP entries to require
all four `libdexkit.so` ABIs in universal and only the matching native library
in each split.

- [ ] **Step 3: Add idempotent GitHub Release publication**

Name assets `HyperAICRBypass-<versionName>-<abi>.apk` and upload universal first.
Query an existing Release by tag: abort if public; if and only if it is a draft,
delete and recreate the complete draft so stale assets cannot survive. Before
publication, re-query the release, require it is still a draft, and require its
asset-name set equals exactly the five expected names. Publish once after those
checks; later runs against that public Release must fail.

- [ ] **Step 4: Validate YAML and inspect the diff**

Parse the workflow with an available YAML parser, inspect `git diff --check`,
and confirm secret values never appear in tracked files.

### Task 4: Generate and configure the permanent release key

**Files:**
- Generate outside repository: `C:/Users/wei.shao/.signing/HyperAICRBypass/HyperAICRBypass-release.p12`
- Generate outside repository: `C:/Users/wei.shao/.signing/HyperAICRBypass/release-key-credentials.txt`
- Back up outside repository: `D:/Backup/SigningKeys/HyperAICRBypass/`

- [ ] **Step 1: Generate random signing credentials and PKCS12 key**

Use RSA 4096, SHA-256, a long validity period, alias `hyperaicrbypass`, and
certificate identity `WayneShao <owner@wayneshao.com>`.

- [ ] **Step 2: Restrict local file access and verify certificate data**

Restrict the signing directory to the current Windows identity and SYSTEM.
Use `keytool -list -v` to verify alias, owner, validity, and SHA-256 fingerprint
without printing the password.

- [ ] **Step 3: Create and verify the separately protected backup**

Copy the PKCS12 and credential record to the D-drive backup directory, apply
the same restricted ACL, and compare SHA-256 hashes of both files between the
working and backup directories. Neither directory may be under a Git worktree.

- [ ] **Step 4: Configure GitHub repository secrets**

If an authenticated GitHub API/UI session is available, upload the base64
keystore and credentials as the four named Actions secrets. Otherwise stop
before creating a tag and report the exact remaining secret configuration.

- [ ] **Step 5: Commit, push, and trigger CI**

Commit tracked changes, push `master`, then create and push tag `1-1.0.0` only
after ACLs, fingerprint checks, backup hash checks, and all Actions secrets are
confirmed. Watch the workflow through completion and
verify the source Release contains five signed APK assets. Document that later
official-store publication uses only the universal APK in the package-named
Xposed Modules Repository and is a separate submission/transfer step.

# Task 1 report

## Status

Task 1 bootstrap files and the offline black and gold welcome shell are complete. A missing `Theme.GoElo` resource in the existing scaffold was added so the manifest can link successfully when the Android toolchain is available.

## Changed files

- Added the Gradle 8.13 wrapper, version catalog, root and app build configuration, ProGuard rules, and project ignore rules.
- Added the launcher manifest, backup and extraction exclusions, restricted FileProvider paths, strings, and `Theme.GoElo` resource.
- Added `MainActivity`, `BlackGoldTheme`, and the launch instrumentation smoke test.
- Added `scripts/Use-Toolchain.ps1` and `docs/build-environment.md` documenting pinned versions, sources, checksums, and environment blockers.

## Commands and outputs

From the project root:

`.\gradlew.bat :app:assembleDebug :app:lintDebug`

Result: failed before Gradle startup because the wrapper could not download Gradle 8.13: `java.net.SocketException: Permission denied: connect` while connecting to `services.gradle.org`.

`. .\scripts\Use-Toolchain.ps1`

Result: the pinned JDK executable is absent at `C:\Users\mxnkilmt\Documents\Codex\Toolchains\jdk-17\bin\java.exe`.

SDK check: `sdkmanager unavailable` because `ANDROID_HOME` was not set in that shell and no Android command-line tools were installed. `adb unavailable`; no device smoke test was run.

## Tests

The prescribed assemble and lint command was attempted and is blocked by missing JDK 17, Android SDK, and unreachable Gradle distribution. `:app:connectedDebugAndroidTest` was not run because no SDK or connected device is available. Static inspection confirms the manifest declares no network or storage permissions and only `MainActivity` is exported.

## Concerns

Build, lint, merged-manifest inspection, and instrumentation remain pending until the documented toolchains are installed and Gradle 8.13 can be fetched. The launch test is intentionally the minimal Task 1 smoke test and expects the welcome labels required by the brief.

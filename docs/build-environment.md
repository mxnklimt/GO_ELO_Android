# Build environment

This project is pinned to Java 17, Android SDK Platform 36, Build Tools 35.0.0, and Gradle 8.13. `scripts/Use-Toolchain.ps1` configures these paths only for the process that runs it; it does not alter system Java or environment variables.

## Required toolchain layout

- `C:\Users\mxnkilmt\Documents\Codex\Toolchains\jdk-17\`
- `C:\Users\mxnkilmt\Documents\Codex\Toolchains\android-sdk\`
- Gradle 8.13 is fetched by the checked-in wrapper into `.local\gradle-cache\`.

Run this from the repository root before a build:

```powershell
. .\scripts\Use-Toolchain.ps1
$sdkManager = Join-Path $env:ANDROID_HOME 'cmdline-tools\latest\bin\sdkmanager.bat'
& $sdkManager --sdk_root=$env:ANDROID_HOME 'platform-tools' 'platforms;android-36' 'build-tools;35.0.0'
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

If the SDK manager prompts for licenses, read and accept them interactively. This project intentionally does not script an automatic `yes` response.

## Sources and integrity

| Component | Source | Version | SHA-256 / verification |
| --- | --- | --- | --- |
| Gradle distribution | https://services.gradle.org/distributions/gradle-8.13-bin.zip | 8.13 | `20f1b176237254a6fc204d8434196fa11a4cfb387567519c61536e8710aed78`, enforced by `gradle-wrapper.properties` |
| Gradle wrapper JAR | https://github.com/gradle/gradle/raw/v8.13.0/gradle/wrapper/gradle-wrapper.jar | 8.13.0 source tag | `81A82AAEA5ABCC8FF68B3DFCB58B3C3C429378EFD98E7433460610FECD7AE45F` |
| JDK | https://adoptium.net/temurin/releases/?version=17 | 17 | Download the Windows x64 archive and record its published SHA-256 before extracting as `jdk-17`. |
| Android command-line tools | https://developer.android.com/studio#command-tools | current Windows package | Verify the published checksum before extracting under `android-sdk\\cmdline-tools\\latest`. |

## Bootstrap check on 2026-09-17

The machine supplied Java 8 only (`1.8.0_201`) and did not contain Android SDK, `adb`, a Gradle installation, or a Gradle cache. The Gradle wrapper JAR and launcher scripts were downloaded from Gradle's tagged GitHub source and verified above. Downloading the 8.13 distribution from `services.gradle.org` / `downloads.gradle.org` was blocked by an upstream HTTP 500 response. No JDK 17 or Android SDK archive could therefore be resolved during bootstrap.

Consequently, build, lint, merged-manifest inspection, and device instrumentation testing are blocked until JDK 17 and the Android SDK are placed in the specified paths and the Gradle 8.13 distribution is reachable. The project configuration remains ready for those checks.

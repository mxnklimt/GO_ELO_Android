# Build environment

This project is pinned to Java 17, Android SDK Platform 36, Build Tools 35.0.0, and Gradle 8.13. `scripts/Use-Toolchain.ps1` configures these paths only for the process that runs it; it does not alter system Java or environment variables.

## Required toolchain layout

- `C:\Users\mxnkilmt\Documents\Codex\Toolchains\jdk-17\`
- `C:\Users\mxnkilmt\Documents\Codex\Toolchains\android-sdk\`
- Gradle 8.13 is fetched by the checked-in wrapper into `.local\gradle-cache\`.

Run this from the repository root before a build:

```powershell
. .\scripts\Use-Toolchain.ps1
$androidCli = 'C:\Users\mxnkilmt\Documents\Codex\Toolchains\android.exe'
& $androidCli --no-metrics --sdk $env:ANDROID_HOME sdk install 'platform-tools' 'platforms;android-36' 'build-tools;35.0.0'
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

If the SDK manager prompts for licenses, read and accept them interactively. This project intentionally does not script an automatic `yes` response.

## Sources and integrity

| Component | Source | Version | SHA-256 / verification |
| --- | --- | --- | --- |
| Gradle distribution | https://services.gradle.org/distributions/gradle-8.13-bin.zip | 8.13 | `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`, enforced by `gradle-wrapper.properties` |
| Gradle wrapper JAR | https://github.com/gradle/gradle/raw/v8.13.0/gradle/wrapper/gradle-wrapper.jar | 8.13.0 source tag | `81A82AAEA5ABCC8FF68B3DFCB58B3C3C429378EFD98E7433460610FECD7AE45F` |
| JDK | https://adoptium.net/temurin/releases/?version=17 | Temurin 17.0.20.1 | `E53A79C3C3D86865BD7E787903884331068E71321714FFD44F145785AFFC7CB0` (download archive) |
| Android CLI | https://developer.android.com/tools/agents/android-cli/download | 1.0.16261425 | `2D64D0D2ED16B7D74DC4EA255D60FCECBF534E0FBA8DD4DF2126C7173298D52D` |

## Installation check on 2026-09-17

The machine initially supplied Java 8 only (`1.8.0_201`) and did not contain Android SDK, `adb`, a Gradle installation, or a Gradle cache. The wrapper JAR and launcher scripts were downloaded from Gradle's tagged GitHub source and verified above. JDK 17 and the Android SDK are now installed at the paths listed above. The Gradle 8.13 distribution is being downloaded into the checked-in wrapper cache; build verification follows once that archive completes.

Build, lint, merged-manifest inspection, and device instrumentation testing still require the Gradle distribution download to complete. The project configuration remains ready for those checks.

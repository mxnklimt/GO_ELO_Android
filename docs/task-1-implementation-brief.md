## Task 1：构建环境与可启动的黑金原生壳

**Files — Create:** `settings.gradle.kts`、`build.gradle.kts`、`gradle/libs.versions.toml`、`gradle.properties`、`gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.properties`、`gradle/wrapper/gradle-wrapper.jar`、`.gitignore`、`app/build.gradle.kts`、`app/proguard-rules.pro`、`app/src/main/AndroidManifest.xml`、`app/src/main/res/values/strings.xml`、`app/src/main/res/xml/backup_rules.xml`、`app/src/main/res/xml/data_extraction_rules.xml`、`app/src/main/res/xml/file_paths.xml`、`app/src/main/java/dev/goelo/android/MainActivity.kt`、`app/src/main/java/dev/goelo/android/ui/theme/BlackGoldTheme.kt`、`scripts/Use-Toolchain.ps1`、`docs/build-environment.md`。

**Test:** `app/src/androidTest/java/dev/goelo/android/LaunchTest.kt`。

**Interfaces:** 产生 `@Composable fun BlackGoldTheme(content: @Composable () -> Unit)` 和可启动 `MainActivity`；后续任务在主题内接入真实 UI。

- [ ] 1. 在执行期重新检查 R 是否存在，创建独立 Git 工程并复制已批准的设计和本计划。`.gitignore` 排除 `.gradle/`、`**/build/`、`local.properties`、`.local/`、`*.jks`、`*.keystore`。不要复制旧项目整个目录。
- [ ] 2. 准备官方 JDK 17、Android 命令行工具、Gradle 8.13 到 `C:/Users/mxnkilmt/Documents/Codex/Toolchains/`。下载清单、来源及校验和记到 `docs/build-environment.md`；JDK 放 `jdk-17/`，SDK 放 `android-sdk/`。出现 SDK 许可条款时由用户阅读接受，不用脚本自动回答 `yes`。只设置当前执行进程环境，不改系统 Java：

```powershell
# scripts/Use-Toolchain.ps1；从工程根目录点入执行
$env:JAVA_HOME = 'C:\Users\mxnkilmt\Documents\Codex\Toolchains\jdk-17'
$env:ANDROID_HOME = 'C:\Users\mxnkilmt\Documents\Codex\Toolchains\android-sdk'
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.local\gradle-cache'
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;" + $env:Path
& "$env:JAVA_HOME\bin\java.exe" -version
```

```powershell
$sdkManager = Join-Path $env:ANDROID_HOME 'cmdline-tools\latest\bin\sdkmanager.bat'
& $sdkManager --sdk_root=$env:ANDROID_HOME 'platform-tools' 'platforms;android-36' 'build-tools;35.0.0'
```

- [ ] 3. 用 Gradle 官方分发生成 wrapper，按版本表填写 version catalog。仓库只有 `google()`、`mavenCentral()` 和插件门户；开启 Compose、KSP、serialization，Java/Kotlin target=17，Room schema 输出到 `app/schemas/` 并纳入 Git。`app/build.gradle.kts` 的关键 Android 配置如下，依赖必须覆盖版本表中 runtime、Room compiler、测试和 Compose 测试组件：

```kotlin
android {
    namespace = "dev.goelo.android"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.goelo.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
```

- [ ] 4. Manifest 不申请 INTERNET 或广泛存储权限；`allowBackup=false`，Android 12+ 的 data extraction rules 排除 cloud-backup 和 device-transfer 的数据库、files、preferences、root 域，旧系统 full-backup 同样排除，避免系统自动备份违背手动离线方案。FileProvider 限定后续 `cache/exports/` 子目录；唯一可导出的 Activity 是 launcher。
- [ ] 5. 写启动 smoke test，先跑并观察缺少界面的失败；测试只能用于引导，Task 5 更新为初始化与真实首页断言：

```kotlin
@RunWith(AndroidJUnit4::class)
class LaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun opensOfflineWelcome() {
        compose.onNodeWithText("GO ELO").assertIsDisplayed()
        compose.onNodeWithText("开始记录").assertIsDisplayed()
    }
}
```

- [ ] 6. 实现最小原生欢迎页面和黑金主题。背景 `0xFF11110F`、卡片 `0xFF1B1B18`、金色 `0xFFD9BD78`、正文 `0xFFF4F0E7`、次级文字 `0xFFAAA79B`；正文至少 14sp、辅助文字至少 12sp、主要触摸区域至少 48dp。不启用系统动态色覆盖黑金方案。
- [ ] 7. `./gradlew.bat :app:assembleDebug :app:lintDebug`；有已连接设备时运行 `:app:connectedDebugAndroidTest`。没有设备时记录“构建可验，设备 smoke 未执行”，执行期准备 API 36 模拟器后完成。检查 merged manifest 无网络权限、入口能启动，再提交 `build: bootstrap offline Android app`。


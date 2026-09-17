$env:JAVA_HOME = 'C:\Users\mxnkilmt\Documents\Codex\Toolchains\jdk-17'
$env:ANDROID_HOME = 'C:\Users\mxnkilmt\Documents\Codex\Toolchains\android-sdk'
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.local\gradle-cache'
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;" + $env:Path
& "$env:JAVA_HOME\bin\java.exe" -version

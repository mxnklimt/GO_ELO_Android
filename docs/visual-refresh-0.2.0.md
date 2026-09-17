# GO ELO 0.2.0 界面更新

沿用已确认的黑金竞技方向，优化现有原生 Compose 页面。

- 统一深墨绿黑底、暖金强调、圆角卡片、文字层级和边框。
- 首页：棋盘暗纹、金色光晕、等级分自适应字号、参考段位与目标进度。
- 走势图：渐变填充、范围切换、触摸参考线与选中节点；保留原始折线，不插值改变走势。
- 分析页：环形胜率、胜负和净变化指标、段位胜率条与月度数据。
- 历史：折叠筛选、胜负色块、旧记录的真实对手名称、赛后分及原始分差。
- 全局：带图标的导航、固定记一盘按钮、短淡入淡出转场、系统安全区。
- 录入、备份和恢复：统一底部面板、可滚动内容、键盘避让。
- 新增黑金围棋自适应桌面图标。

包名仍为 dev.goelo.android，versionCode 从 1 提升为 2，versionName 为 0.2.0。
没有修改数据库结构或评分规则。新旧 APK 的签名证书 SHA-256 一致，可覆盖安装；无需卸载。

## 验证

运行：

```powershell
. .\scripts\Use-Toolchain.ps1
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --offline --console=plain
```

结果：构建通过；24 项 JVM 单元测试通过；lint 0 错误、11 警告（依赖更新、旧未用字符串及图标提示）；设备测试源码编译通过。

本机没有连接的安卓设备，也未配置模拟器。本轮没有执行设备测试或实机截图验证；大字体、键盘和不同机型的视觉效果仍需实机检查。

安装包：dist/go-elo-debug.apk。

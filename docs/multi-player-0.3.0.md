# GO ELO Android 0.3.0 — 多棋手与双方记分

2026-09-18

## 使用

1. 顶部「棋手 · 当前姓名 / 切换 / 添加」打开棋手列表。支持搜索、新增和切换。
2. 添加时填写姓名与初始 ELO，也可用 1–9 段基准分填入。同名允许，使用独立 ID 区分。
3. 点击「记一盘」→「已有棋手」→选择对手→选择当前棋手胜或负。双方分数和战绩在同一事务更新。
4. 切换到任一棋手，即可查看其成长、分析、历史和设置。
5. 在任一方历史中更正胜负或删除该场对局，双方及后续关联对局同步重算；刚录入的记录可撤销。
6. 「临时对手」保留段位＋最近不超过 20 盘战绩的估分规则，只更新当前棋手。
7. 备份文件包含全部棋手和全部对局，恢复预览显示总量，恢复时整体替换。

## 数据兼容

- Room 数据库升级到 v2：原个人档案、对局及精确历史分差保留，旧对局归原棋手 local。
- 备份升级到 schemaVersion 2，仍读取原 schemaVersion 1 的 .goelo.json。
- 不根据旧历史中的对手姓名自动创建档案或猜测对手当前 ELO。
- 已有对手按双方赛前 ELO、K=20 更新；全精度计算。临时对手规则不加平滑；新记录使用 `elo-v2` 的系数 100，历史 `elo-v1` 记录继续使用系数 400。编辑旧临时对手记录时会按当前规则重新计算。
- 同一场比赛在全局保存一次，各棋手页面按自身视角显示。
- 源码与安装包保存在本地 Git 仓库；尚无 Git remote，未推送 GitHub。

## 验证

最终命令：

```powershell
. ./scripts/Use-Toolchain.ps1
# 在独立 worktree 构建时，指向已有 Gradle 缓存
$env:GRADLE_USER_HOME = 'C:/Users/mxnkilmt/Documents/Codex/Projects/GO_ELO_Android/.local/gradle-cache'
./gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --offline --console=plain '-Pkotlin.compiler.execution.strategy=in-process'
```

- BUILD SUCCESSFUL。37 项 JVM 测试，无失败/错误/跳过；Lint 0 errors、5 warnings（依赖更新及原有资源提示）。
- 测试覆盖等分双方变为 2010/1990、切换与临时记录隔离、三人级联更正/删除、撤销、重复请求、自我对局及不存在对手拒绝、初始分修改、旧备份升级、多棋手备份往返及错误关联/篡改分值拒绝。
- 宿主 SQLite 执行生产 MIGRATION_1_2 SQL，旧分值与 revision 保留，新增列/default 与导出 v2 schema 对齐。v1 schema 原样保留。
- 新增 Android Room 迁移/重开/回滚测试及添加棋手→已有对手记分→第二方历史 UI 测试，均编译通过。
- 独立只读代码审查无必须修复问题；建议后续扩展第二方历史更正/删除 UI 测试。
- adb 未连接设备；Android 设备测试未执行，实机视觉和交互仍未验收。

## 安装包

- applicationId: dev.goelo.android
- versionName: 0.3.0；versionCode: 3
- 沿用已安装 0.2.0 的原调试签名，可覆盖升级，无需卸载。
- 证书 SHA256: cb0498ee1a305576d49e07e6c28b0ed55909771b2b4dd555b38dece107388efa
- APK SHA256: E62405E96A85B8925184E89A4E0C81B6FE69C22D7DB2411B9AB87C3E90F25481
- 调试签名路径通过 GOELO_DEBUG_KEYSTORE 配置，密钥未加入仓库。

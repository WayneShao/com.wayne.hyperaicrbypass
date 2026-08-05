# Hyper AICR Bypass

一个用于小米相册本地 AI 分析（AICR）的可配置 LSPosed 模块。它允许设备所有者独立控制 AICR 的运行门槛，并把相册分析进度显示到小数点后三位。

> Hyper AICR Bypass is a configurable LSPosed module for Xiaomi Gallery AICR. It lets device owners control local AI-analysis eligibility policies independently and shows indexing progress with three decimal places.

## 项目缘起

这个项目来自一个很直接的问题：在我自己的小米设备上，相册的本地 AI 处理长期无法触发。即使设备处于看起来合适的状态，AICR 仍可能因为温度、电量、充电、息屏、运行次数、任务调度等内部条件拒绝启动或中途停止。这些条件过于苛刻，界面也没有给出足够清楚的原因。

在正常入口无法可靠启动本地分析后，我不得不使用 LSPosed Hook 作为最后手段。设备和数据属于用户，是否允许设备继续执行本地分析，也应当由用户自己决定。

本模块只改变资格判断和进度文本显示：它不会提高模型或索引算法的计算速度，不会伪造“分析完成”，也不会为精确进度读取或修改 AICR 数据库。

## 功能

- 原生设置界面，可随时关闭总开关并保留各项选择。
- “全选”模式与逐项配置互斥；取消全选后可独立调整每个门槛。
- 某项 Hook 在当前 AICR 版本中无法安全定位时，该项会显示 `Unavailable` 并置灰，不影响其他已经成功安装的 Hook。
- 可主动要求重新适配当前 AICR 版本。
- 可隐藏启动器图标；隐藏后仍可从 LSPosed 的模块设置入口打开界面。
- 打开设置时检测 modern Xposed 服务；模块未启用时提示用户并立即退出。
- 相册分析页面和全局 AI 搜索设置页面显示三位小数进度，例如 `69.864%`，使用四舍五入。

可独立控制的 11 类策略：

| 策略 | 作用 |
| --- | --- |
| 温度门槛 | 绕过启动与运行中的超温判断 |
| 充电状态 | 满足必须充电的启动条件 |
| 电量门槛 | 绕过低电量暂停或拒绝启动 |
| 息屏与空闲 | 绕过交互状态与设备空闲限制 |
| 换机迁移 | 绕过迁移任务占用判断 |
| 每日运行次数 | 绕过每日分析次数上限 |
| 单次运行时长 | 绕过最长运行时长停止条件 |
| 运行间隔 | 绕过两次分析之间的等待时间 |
| 系统过载 | 绕过系统繁忙场景暂停判断 |
| 后台任务约束 | 移除相关 JobScheduler 与 WorkManager 限制 |
| AI 分析界面 | 始终向相册声明支持 AI 分析进度界面 |

## 版本适配

模块优先使用当前已知的精确类名和方法签名。精确 Hook 点不存在时，才使用 DexKit 根据包名、返回值、完整参数形状、静态/实例属性和代码字符串锚点进行语义查找。

语义查找只接受唯一且通过完整校验的候选方法。没有候选或存在歧义时，该 Hook 会保持禁用，而不是猜测一个方法后继续执行。更新 AICR 后可以在模块界面点击“重新适配当前 AICR 版本”清除旧映射并重新发现。

动态适配只能降低小幅重命名或混淆变化带来的维护成本，不能保证兼容尚未发布或发生重大逻辑重构的版本。

## 已验证环境

以下是当前实机验证组合，不代表最低要求，也不构成对其他版本的兼容承诺：

| 项目 | 版本 |
| --- | --- |
| 设备 | Xiaomi `nezha`（25128PNA1C） |
| Android | 16 |
| HyperOS | `OS3.0.307.6.WPACNXM` |
| 进程 ABI | `arm64-v8a` |
| AICR | `4.0.6`（versionCode `2030040006`） |
| 小米相册 | `5.0.7.7-0720-R`（versionCode `5000707`） |
| AI Service | `3.12.2_dd2be79_260427_cn`（versionCode `312002`） |
| LSPosed / Vector | `2.1.1`，libxposed API `102` |

模块应用最低支持 Android 9（API 28），框架必须支持 modern libxposed API 102。实际 Hook 兼容性仍取决于设备 ROM、相册、AICR 和 AI Service 的具体实现。

## 安装与作用域

旧的本地调试模块使用包名 `com.example.hyperaicrbypass`，与正式版是两个不同应用，不能覆盖迁移。安装正式版前应卸载旧模块，再安装 `com.wayne.hyperaicrbypass` 并在 LSPosed 中重新启用模块和作用域。从正式版 1.0.0 开始，公开版本会持续使用同一把永久 Release 密钥，可以正常覆盖升级。

1. 安装 Release 中的 `universal` APK。除非明确知道被 Hook 目标进程的 ABI，否则不要选择分架构 APK。
2. 在支持 libxposed API 102 的 LSPosed / Vector 中启用模块。
3. 模块使用静态作用域模式，管理器只会显示以下三个固定作用域，无需由模块动态申请：

```text
com.miui.gallery
com.xiaomi.aicr
com.xiaomi.aiservice
```

4. 结束并重新启动上述作用域中的应用进程，然后重新打开小米相册。当前作用域不包含“Android 系统”，通常不需要重启手机或 LSPosed。
5. 打开模块设置，确认所需策略不是 `Unavailable`；更新 AICR 后执行一次“重新适配当前 AICR 版本”。

分架构 APK 必须匹配被 Hook 目标进程的 ABI，而不只是手机硬件支持的 ABI。64 位设备也可能运行 32 位目标进程；不确定时始终使用 `universal`。

## 精确进度说明

模块从 AICR 当前计算所使用的原始计数中生成显示快照，并沿用 AICR 自己的进度传输链路把快照带到界面进程。百分数固定保留三位小数并使用 `HALF_UP` 四舍五入，范围限制为 `0.000%` 到 `100.000%`。

精确值只替换文字和无障碍描述。AICR 原有的整数 `analyse_progress`、进度条、扫描状态、持久化数据和统计逻辑保持不变。当精确快照与 AICR 当前整数进度不一致或已经过期时，界面回退到原始整数显示。

## 隐私

模块自身：

- 不申请 `INTERNET` 权限；
- 不包含上传、遥测或用户行为分析代码；
- 设置保存在本机，通过受调用方校验的 ContentProvider 提供给作用域进程；
- 精确进度功能不查询、复制或修改 AICR 数据库。

这些说明只描述 Hyper AICR Bypass 自身。被 Hook 的小米相册、AICR 和 AI Service 如何处理数据，仍由对应系统应用及其版本决定。

## 风险提示

温度、电量、充电、系统负载和后台调度限制通常用于控制功耗、发热和稳定性。绕过这些条件可能造成明显发热、快速耗电、后台任务争用、应用崩溃、系统重启，极端情况下也可能导致尚未落盘的数据丢失。

请只启用自己理解且确实需要的策略，并自行监控设备状态。模块不会替代 Android、内核或硬件层的最终保护机制，但也不应把这些保护机制当成日常高负载运行的保证。

## 从源码构建

环境要求：JDK 21、Android SDK 37.0、支持 SDK 37 的 Android Gradle Plugin。

```bash
./gradlew testDebugUnitTest assembleDebug
```

生成 universal 与四种 ABI 的未签名 Release 构建：

```bash
./gradlew clean testDebugUnitTest assembleRelease -PsplitAbi=true
```

正式产物由 GitHub Actions 使用仓库 Secrets 中的永久 Release 密钥签名。不要使用临时密钥发布，也不要把 keystore 或口令提交到仓库。

## Release

tag 必须与 APK 内版本严格对应，格式为：

```text
<versionCode>-<versionName>
```

首个正式版本 `versionCode=1`、`versionName=1.0.0`，对应 tag：

```text
1-1.0.0
```

modern libxposed API 102 版本为 `versionCode=2`、`versionName=1.1.0`，对应 tag：

```text
2-1.1.0
```

tag 推送后，Release 工作流会运行单元测试，构建 universal、`arm64-v8a`、`armeabi-v7a`、`x86`、`x86_64` 五个 APK，使用同一正式证书签名并逐个验签。所有检查完成前 Release 保持为 draft；已经公开的同版本 Release 不会被覆盖。

普通用户应下载 universal APK。分架构包仅供明确知道目标进程 ABI 的用户使用。

## 上架 Xposed Modules Repository

当前仓库中的 Release 是源码仓库发布，不会自动出现在 Xposed Modules Repository。后续上架时，按[官方提交说明](https://github.com/Xposed-Modules-Repo/submission)提交 `[submission] com.wayne.hyperaicrbypass`，或按官方 transfer 流程转移现有仓库。

官方 package-named 仓库的 Release 只上传经过同一证书签名的 universal APK，并保持相同的 `versionCode-versionName` tag。不要只替换已经发布的 APK 资源；任何二进制变化都应提高应用版本并创建新 tag，以保证官方仓库能够检测更新。

## License

[MIT](LICENSE)


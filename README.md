# Hyper AICR Bypass

一个用于小米相册本地 AI 分析（AICR）的可配置 LSPosed 模块。它允许设备所有者独立控制 AICR 的运行门槛、暂停本地分析，并以可选精度显示相册分析进度。

> Hyper AICR Bypass is a configurable LSPosed module for Xiaomi Gallery AICR. It lets device owners control local AI-analysis eligibility, pause local indexing, and select the displayed progress precision.

## 项目缘起

这个项目来自一个很直接的问题：在我自己的小米设备上，相册的本地 AI 处理长期无法触发。即使设备处于看起来合适的状态，AICR 仍可能因为温度、电量、充电、息屏、运行次数、任务调度等内部条件拒绝启动或中途停止。这些条件过于苛刻，界面也没有给出足够清楚的原因。

在正常入口无法可靠启动本地分析后，我不得不使用 LSPosed Hook 作为最后手段。设备和数据属于用户，是否允许设备继续执行本地分析，也应当由用户自己决定。

本模块只改变资格判断和进度文本显示：它不会提高模型或索引算法的计算速度，不会伪造“分析完成”，也不会修改 AICR 数据库。为了避免页面先显示整数，打开进度界面时会要求 AICR 执行它原有的实时本地计数，而不是先使用旧的整数缓存。

## 功能

- 原生设置界面，提供“全局绕过”“禁用 AI 分析”和原逻辑三种互斥运行状态；两个开关也可以同时关闭。
- 禁用模式会暂停当前 AICR 任务并阻止新的启动请求，可选择在 USB、交流或无线外部供电时按已选门槛恢复运行。
- “全选”模式与逐项配置互斥；取消全选后可独立调整每个门槛。
- 某项 Hook 在当前 AICR 版本中无法安全定位时，该项会显示 `Unavailable` 并置灰，不影响其他已经成功安装的 Hook。
- 可主动要求重新适配当前 AICR 版本。
- 可隐藏启动器图标；隐藏后仍可从 LSPosed 的模块设置入口打开界面。
- 打开设置时检测 modern Xposed 服务；模块未启用时提示用户并立即退出。
- 精细进度与运行模式独立，可关闭或选择十分位、百分位、千分位，两个 AICR 进度界面使用相同的 `HALF_UP` 四舍五入规则。
- 可将 AICR“复制网址”识别结果交给系统默认浏览器或指定浏览器；候选列表只包含同时声明浏览器入口并支持 HTTP/HTTPS 的应用。

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

这里的 3.x / 4.x 指 AICR 应用自身版本，不是 HyperOS 版本。当前已知分支按 AICR 3.x 与 AICR 4.x 分别维护精确签名，并共享严格的动态发现回退。AICR 4.x 内部同时存在可读类名布局与紧凑混淆布局，模块会根据运行时类结构选择对应目录，不把某个小版本号写死为唯一分支条件。

模块优先使用当前已知的精确类名和方法签名。精确 Hook 点不存在时，才使用 DexKit 根据目标布局、返回值、完整参数形状、静态/实例属性和代码字符串锚点进行语义查找。AICR 的主进程、搜索数据、搜索界面、搜索服务和无关辅助进程分别安装自己需要的 Hook，避免在每个进程重复执行整套发现逻辑。

语义查找只接受唯一且通过完整校验的候选方法。没有候选或存在歧义时，该 Hook 会保持禁用，而不是猜测一个方法后继续执行。更新 AICR 后可以在模块界面点击“重新适配当前 AICR 版本”清除旧映射并重新发现。

动态适配只能降低小幅重命名或混淆变化带来的维护成本，不能保证兼容尚未发布或发生重大逻辑重构的版本。

省电控制单独验证三个关键 Hook 点：`checkCanStart(int)`、`getNeedStop()` 和数据库 Provider 启动入口；数据库入口同时覆盖普通与无限制两种启动请求。只有这条关键链全部安装成功时，设置页才允许新开启“禁用 AI 分析”。UI Provider 的状态修正是可选的视觉增强，不影响执行安全覆盖判定。重新适配期间显示为等待状态；适配失败时会保留已经保存的选择，但仍允许用户关闭禁用模式，不会把部分 Hook 描述成完整保证。

## 已验证环境

以下是当前实机验证组合，不代表最低要求，也不构成对其他版本的兼容承诺：

| 项目 | 版本 |
| --- | --- |
| 设备 | Xiaomi `nezha`（25128PNA1C） |
| Android | 17（API 37） |
| HyperOS | `OS4.0.0.10.XPACNXM` |
| 进程 ABI | `arm64-v8a` |
| AICR | `4.11.20`（实机 11/11、12/12、2/2）；历史验证见下文 |
| 小米相册 | `5.4.1.19-0801-R`（versionCode `5040119`） |
| AI Service | `4.2.5_3092628_260729_cn`（versionCode `402005`） |
| LSPosed / Vector | `2.1.1`，libxposed API `102` |

模块应用最低支持 Android 9（API 28），框架必须支持 modern libxposed API 102。当前版本已在 AICR `4.11.20`（versionCode `2030041120`）完成实机注入与状态页回归：11 类门槛策略 `11/11`、精细进度 `12/12`、复制网址 `2/2`、省电执行边界 `2/2`。AICR `3.63.0`（`11/11`、`12/12`、`2/2`）和 `4.0.6`（`11/11`、`15/15`、`1/1`）为 1.2.0 发布时的历史实机结果；本次保留对应精确目录与回归测试，但没有在当前系统上重新安装旧版复测。实际 Hook 兼容性仍取决于设备 ROM、相册、AICR 和 AI Service 的具体实现。

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
5. 打开模块设置，确认所需策略不是 `Unavailable`；“禁用 AI 分析”可开启前还需要暂停链显示可用。更新 AICR 后执行一次“重新适配当前 AICR 版本”。

分架构 APK 必须匹配被 Hook 目标进程的 ABI，而不只是手机硬件支持的 ABI。64 位设备也可能运行 32 位目标进程；不确定时始终使用 `universal`。

## 省电模式

开启“禁用 AI 分析”后，模块在 AICR 已确认的执行边界请求停止当前循环，同时拦截数据库 Provider 的普通和无限制启动请求。来自 UI 的“开始”操作会转换为暂停状态，停止、完成、进度查询和其他 Provider 调用保持原样。模块不会清除 AICR 数据、重建索引或把强制结束进程作为日常控制手段。

“外部供电时允许运行”以 Android 报告的 USB、交流或无线供电连接为准，包含满电后仍连接电源的状态。接通电源时进入“按已选门槛绕过”，拔掉电源后立即恢复暂停判定；正在执行的循环会在下一个已确认的停止边界退出。无法读取供电状态时按未连接处理。

## 精确进度说明

模块从 AICR 当前计算所使用的原始计数中生成显示快照，并沿用 AICR 自己的进度传输链路把快照带到界面进程。进入相册分析页或全局 AI 搜索设置页时，模块会跳过 AICR 的两级整数缓存，先完成一次原生实时计数，避免第一帧在整数和小数之间跳变。用户可以关闭精细显示，或选择保留一、二、三位小数；所有精度都使用 `HALF_UP` 四舍五入，并限制在 `0%` 到 `100%`。

精确值只替换文字和无障碍描述。AICR 原有的整数 `analyse_progress`、进度条、扫描状态、持久化数据和统计公式保持不变。同一运行代次内，即使 AICR 随后只发送状态而没有再次携带精确载荷，模块也会在最多六分钟内保留最后一个与原生整数一致的已验证精确值。开始或暂停造成运行代次切换时，只允许由 AICR 明确标记的状态切换继承一次仍然有效的值，随后由实时统计替换。

关闭精细显示时，模块保留 AICR 原始文字，也不执行精细显示专用的缓存绕过和强制通知。开启后，模块不会把整数 `N%` 补成虚假的 `N.000%`。如果当前 AICR 版本无法定位真实计数、载荷损坏或原生整数无法由捕获的计数重放，模块会保留 AICR 原始显示并记录 Hook 失败；其他门槛绕过和省电功能仍可独立工作。

## 隐私

模块自身：

- 不申请 `INTERNET` 权限；
- 不包含上传、遥测或用户行为分析代码；
- 设置保存在本机，通过受调用方校验的 ContentProvider 提供给作用域进程；
- 精确进度只复用 AICR 在作用域进程内执行的本地计数，不读取图片内容，不复制或修改 AICR 数据库，也不会把计数发送到网络。

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

省电控制、可配置精细进度及首屏状态切换修复为 `versionCode=3`、`versionName=1.1.1`，对应 tag：

```text
3-1.1.1
```

AICR 3.x / 4.x 双分支适配、Hook 可用状态及复制网址浏览器选择为 `versionCode=4`、`versionName=1.2.0`，对应 tag：

```text
4-1.2.0
```

HyperOS 4 / Android 17 所带 AICR 4.11.20 紧凑布局适配、分进程发现、跨版本上报排序及重扫幂等修复为 `versionCode=5`、`versionName=1.3.0`，对应 tag：

```text
5-1.3.0
```

tag 推送后，Release 工作流会运行单元测试，构建 universal、`arm64-v8a`、`armeabi-v7a`、`x86`、`x86_64` 五个 APK，使用同一正式证书签名并逐个验签。若 `.github/release-notes/<versionName>.md` 存在，工作流会直接使用其中的完整版本说明；否则才生成提交记录。所有检查完成前 Release 保持为 draft；已经公开的同版本 Release 不会被覆盖。

普通用户应下载 universal APK。分架构包仅供明确知道目标进程 ABI 的用户使用。

## 上架 Xposed Modules Repository

当前仓库中的 Release 是源码仓库发布，不会自动出现在 Xposed Modules Repository。后续上架时，按[官方提交说明](https://github.com/Xposed-Modules-Repo/submission)提交 `[submission] com.wayne.hyperaicrbypass`，或按官方 transfer 流程转移现有仓库。

官方 package-named 仓库的 Release 只上传经过同一证书签名的 universal APK，并保持相同的 `versionCode-versionName` tag。不要只替换已经发布的 APK 资源；任何二进制变化都应提高应用版本并创建新 tag，以保证官方仓库能够检测更新。

## License

[MIT](LICENSE)


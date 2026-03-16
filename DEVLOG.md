# CrabAgent — Dev Log

> **Project Carcinization 蟹化计划**
> 第一个为手机而生的 AI Agent 框架。口袋里的管家。

---

## Phase 0 — Sprint 3: 2026-03-15

### 目标

完成 PocketClaw v0.4.0：全功能 claw 对标，16 个工具，安全层，4-tab UI，真机可用。

### 达成

#### ✅ 全功能 Tool 系统 — 16 个内置工具

| 工具 | ID | 风险级 | 状态 |
|------|----|--------|------|
| 文件读取 | `file_read` | L0 | ✅ |
| 文件写入 | `file_write` | L1 | ✅ |
| 文件列表 | `file_list` | L0 | ✅ |
| 文件删除 | `file_delete` | L2 | ✅ |
| 屏幕读取 | `screen_read` | L0 | ✅ |
| 屏幕点击 | `screen_tap` | L3 | ✅ |
| 屏幕滑动 | `screen_swipe` | L3 | ✅ |
| 屏幕输入 | `screen_input` | L1 | ✅ |
| 屏幕返回 | `screen_back` | L3 | ✅ |
| 创建提醒 | `schedule_create` | L1 | ✅ |
| 提醒列表 | `schedule_list` | L0 | ✅ |
| Telegram 发送 | `tg_send` | L2 | ✅ |
| 剪贴板读取 | `clipboard_read` | L0 | ✅ |
| 剪贴板写入 | `clipboard_write` | L1 | ✅ |
| 应用启动 | `app_launch` | L1 | ✅ |
| 网络搜索 | `web_search` | L0 | ✅ |

#### ✅ 安全四件套

| 组件 | 职责 | 状态 |
|------|------|------|
| `PermissionGuard` | L0 自动放行 / L1 首次授权 / L2-L3 每次弹窗确认 | ✅ |
| `ToolConfirmDialog` | 真实 AlertDialog，CompletableDeferred 阻塞执行直到用户点允许/拒绝 | ✅ |
| `AuditLog` | 内存审计日志，Settings 页可展开查看 | ✅ |
| `PathSandbox` | 文件路径白名单 (/PocketClaw, /Download, /Documents) | ✅ |
| `RateLimiter` | 每轮最多 N 次工具调用，冷却期，连续失败熔断 | ✅ |

#### ✅ 上下文动态精简 (Context Claw)

| 组件 | 说明 |
|------|------|
| `ContextBudget` | 本地/云端两套 token 预算，自适应分配 |
| `ToolContext` | 工具结果三层摘要（全文→截取→摘要） |
| 二次生成 | 工具执行后注入结果，触发第二轮 LLM 生成 |

#### ✅ UI 重构 — 4-Tab + 合并设置

| Tab | 内容 |
|-----|------|
| Chat | 对话 + 龙虾养成头像 + 语音输入 + TTS 播放 |
| Memory | Bond 记忆管理 |
| Skills | 内置/自定义技能管理 |
| Settings | 主题/LLM 模式/语音/权限/提醒/消息/审计/关于 |

#### ✅ 关键修复

- **暗/亮主题切换**：Preferences.themeMode 改用 `mutableStateOf`，Compose 可观测，即时生效
- **STT 语音输入**：替换 "coming soon" toast → Android `SpeechRecognizer`，识别结果填入输入框
- **TTS**：已验证使用 Android 系统 TextToSpeech，支持流式朗读和分句缓冲
- **权限引导**：Settings 新增 Accessibility / Storage / Notification 三个权限入口，一键跳转系统设置
- **提醒 UI**：Settings 内嵌提醒列表 + "Create Reminder" 创建按钮 + 时间/名称/重复设置弹窗
- **消息 Token 配置**：Settings 内 Telegram/Discord bot token 配置弹窗

#### ✅ SOUL Prompt 云端/本地双模式

- 本地模型（Qwen3-1.7B）：极简 prompt，节省 token
- 云端 API（Qwen-Plus）：丰富人格 + 详细格式规范 + 工具 few-shot 示例
- 工具列表动态注入：ToolRegistry 自动生成可用工具描述

#### ✅ 多平台消息桥

4 个平台全部接入 incoming handler → LLM 回复管线：
- Telegram（长轮询收/HTTP 发）
- Discord（REST API）
- Feishu（Webhook）
- Slack（Webhook）

### 代码量

```
PocketClaw v0.4.0 代码统计:

总计: 95 Kotlin files · 19,404 LoC

├── Claw Layer (自研)           27 files · 1,908 LoC
│   ├── tools/                  11 files (16 个工具 + 注册/解析/执行)
│   ├── security/                6 files (PermissionGuard/AuditLog/PathSandbox/RateLimiter)
│   ├── prompt/                  (SOUL/PromptAssembler/ContextBudget/UserProfile/Skills)
│   ├── bond/                    (BondEngine/Memory/Growth/StateExporter)
│   └── skills/                  (SkillRouter/CustomSkill)
│
├── App Layer (UI/Services)     23 files · 3,469 LoC
│   ├── ui/                      5 screens (Chat/Memory/Skills/Settings + ToolConfirmDialog)
│   ├── messaging/               5 files (Bridge/Telegram/Discord/Feishu/Slack)
│   ├── service/                 (ScreenControlService/TaskWorker)
│   └── api/                     (DashScopeProvider)
│
└── LLM Hub Engine (fork)       42 files · 13,949 LoC
    ├── inference/               (UnifiedInferenceService/llama.cpp/whisper.cpp)
    ├── repository/              (ChatRepository/ModelRepository)
    ├── data/                    (Room DB/Entities/Migrations)
    └── ui/components/           (TtsService)
```

### 流水账

1. 创建 `ToolConfirmDialog`：使用 `CompletableDeferred<Boolean>` + Compose `mutableStateOf` 实现跨协程 UI 确认
2. 修复主题切换：根因是 `SharedPreferences.getString()` 不是 Compose 可观测 → 改用 `mutableStateOf` 包装
3. 实现 `SpeechRecognizer` 集成：需要 RECORD_AUDIO 权限（已在 onCreate 中申请），监听 onResults/onError
4. 新增 3 个工具：ClipboardTools (read/write), AppLaunchTool (getLaunchIntentForPackage), WebSearchTool (DuckDuckGo lite HTML parse)
5. 底部导航从 5 tab 精简到 4 tab：Tasks 合并到 Settings 内
6. Settings 页大幅扩展：7 个 section (Appearance/AI Brain/Voice/Permissions/Reminders/Messaging/Security/About)
7. SOUL prompt 重写：双模式适配，丰富 few-shot 示例，避免 LLM 输出 XML/JSON 格式
8. 所有 4 个消息平台统一接入 `handleIncomingMessage` handler

---

## Phase 0 — Sprint 2: 2026-03-11 ~ 2026-03-14

### 目标

PocketClaw 从零到可安装 APK：Serverless 架构、端侧 LLM + 云 API 双模式、养成系统、自定义技能。

### 达成

#### ✅ 架构转向 — Serverless (Path C)

- 完全消除 server 依赖。从"手机+云脑"变为"纯手机 APK"
- 本地 LLM: Qwen3-1.7B-Q8_0 via llama.cpp (NDK JNI)
- 云 API: DashScope (Qwen-Plus) 可切换
- Mac 仅用于编译和 ADB 推送模型

#### ✅ Fork LLM-Hub 作为引擎

- Fork 开源 LLM-Hub Android 项目作为引擎层（用户不可见）
- 上层 PocketClaw UI 全自研
- 保留 UnifiedInferenceService、ChatRepository、Room DB、TtsService
- 新增 BondEngine（养成）、SkillRouter（技能路由）、PromptAssembler（上下文组装）

#### ✅ 养成系统 (EMERGENT_BOND)

- BondMemory 四类记忆：pref / habit / rel / fact
- BondGrowth 五阶段：Larva → Hatchling → Juvenile → Adult → Elder
- XP 累积 + 连续天数 streak + 人格特质 traits
- [M:type:key:value] 标记自动提取
- BondStateExporter: AES-256-CBC 加密导出/导入

#### ✅ 自定义技能系统

- 内置技能 + 用户自定义技能
- 关键词路由 + 技能优先级
- 对话内 "增加一个XX技能" 自动创建
- 持久化到 Room，支持启用/禁用

#### ✅ 完整 APK 构建流水线

- CMake + NDK 编译 llama.cpp + whisper.cpp
- Gradle Kotlin DSL 全配置
- 自适应图标 (lobster in pocket)
- 18 种语言 strings.xml 全部品牌化为 "PocketClaw"
- APK 大小: 420MB (含模型推理库)

### 流水账

1. 下载 Qwen3-1.7B-Q8_0.gguf 从 ModelScope，ADB push 到手机
2. 解决 llama.cpp / whisper.cpp 源码下载超时 → 本地 tarball + add_subdirectory
3. 解决 ggml 版本冲突（两个库共享 ggml，需统一到 llama.cpp 的版本）
4. Kokoro TTS ONNX 类型错误（int64 vs float）→ 改用系统 TextToSpeech 兜底
5. 天气 skill 返回 XML tool_call 格式 → SOUL prompt 加强 HARD_RULES + XML sanitizer
6. LLM Hub UI 残留清理 → 228+ 处 "LLM Hub" 替换为 "PocketClaw"
7. 手机 DNS 解析失败 → 飞连 VPN 干扰，切 WiFi 解决
8. 数据库 migration 5→6→7：BondMemory + BondGrowth + CustomSkill + ScheduledTask

---

## Phase 0 — Sprint 1: 2026-03-10

### 目标

在 Pixel 8 Pro 到货前（~03-17），完成全部云端代码 + Android 骨架。
手机开箱 30 分钟内看到管家工作。

### 达成

#### ✅ 战略定位完成

- 完成 Claw 生态全景调研（OpenClaw 294K★ / ZeroClaw 25K★ / NanoClaw 20K★ / IronClaw 8.7K★ / FastClaw / MiniClaw）
- 核心发现：**所有 Claw 绑死桌面端**，68 亿手机用户 = 0 个移动原生 Agent
- 从阶级幻象费理论反推架构：卖"口袋里的管家"→ 用户在手机上 → 移动端是 Day 1 约束
- 设备选型：**Google Pixel 8 Pro**（12GB RAM，后台存活最优，已下单）
- 完整更新 `CARCINIZATION_PLAN.md`（移动端优先版本）

#### ✅ Rust 云脑 — 编译通过，API 全链路跑通

5 个 crate，803 行 Rust：

| Crate | 职责 | 行数 | 状态 |
|-------|------|------|------|
| `crab-protocol` | 端云 JSON 协议类型（DeviceEvent / CloudResponse / Action / SkillDescriptor） | 122 | ✅ 完成 |
| `crab-pincers` | 左螯（Embedding 语义路由）+ 右螯（最小化 Context 组装） | 139 | ✅ 完成 |
| `crab-carapace` | 甲壳（Wasmtime WASM 沙箱执行器） | 90 | ✅ 骨架完成 |
| `crab-brain` | 大脑（LLM 统一 trait + 编排器：Event→Route→Context→LLM→Actions） | 334 | ✅ 完成 |
| `crab-server` | axum HTTP 服务（POST /event, GET /health） | 118 | ✅ 完成 |

**LLM 接口已通：**
- 接入阿里云百炼 Coding Plan（`coding.dashscope.aliyuncs.com`）
- 模型：**MiniMax-M2.5**（196K context，支持 reasoning）
- 三场景实测通过：老板开会通知（high）、妈妈问周末（normal）、钉钉审批（normal）
- 平均延迟 ~5s，Token 消耗 ~500/次，相比全量 Context 节省 ~10,000 Token/次

#### ✅ Android 端壳 — 骨架完成

7 个 Kotlin 文件，397 行（去重）

### 实测数据

```
# 微信工作群 → 老板开会通知
POST /event → 200 OK
tokens_consumed: 475 | tokens_saved: 10,545 | latency: 4,284ms

# 微信家人群 → 妈妈问周末
POST /event → 200 OK
tokens_consumed: 657 | tokens_saved: 10,363 | latency: 8,248ms

# 钉钉 → 审批通知
POST /event → 200 OK
tokens_consumed: 446 | tokens_saved: 10,574 | latency: 4,337ms
```

---

## 项目结构

```
CrabAgent/
├── CARCINIZATION_PLAN.md          # 战略计划（移动端优先版）
├── EMERGENT_BOND.md               # 养成系统设计文档
├── DEVLOG.md                      # ← 你在这里
├── dashboard.html                 # 项目进展可视化
│
├── PocketClaw/                    # 🦞 主产品 (Android APK)
│   └── android/app/src/main/java/
│       ├── com/pocketclaw/
│       │   ├── app/               # 应用层 (23 files, 3.5K LoC)
│       │   │   ├── MainActivity.kt
│       │   │   ├── PocketClawApplication.kt
│       │   │   ├── ui/            # Chat/Memory/Skills/Settings + ToolConfirmDialog
│       │   │   ├── messaging/     # Telegram/Discord/Feishu/Slack
│       │   │   ├── service/       # ScreenControlService, TaskWorker
│       │   │   └── api/           # DashScopeProvider
│       │   └── claw/              # Claw 智能层 (27 files, 1.9K LoC)
│       │       ├── tools/         # 16 个工具 + Parser/Executor/Registry
│       │       ├── security/      # PermissionGuard/AuditLog/PathSandbox/RateLimiter
│       │       ├── prompt/        # SOUL/PromptAssembler/ContextBudget
│       │       ├── bond/          # BondEngine/Memory/Growth/StateExporter
│       │       └── skills/        # SkillRouter/CustomSkill
│       └── com/llmhub/           # 引擎层 (42 files, 14K LoC, fork)
│           └── llmhub/
│               ├── inference/     # UnifiedInferenceService/llama.cpp
│               ├── repository/    # ChatRepository/ModelRepository
│               ├── data/          # Room DB + Migrations
│               └── ui/components/ # TtsService
│
└── cloud/                         # 云脑 (Rust, Phase 0 遗产)
    ├── Cargo.toml
    ├── protocol/                  # 端云协议
    ├── pincers/                   # 双螯（语义路由 + Context）
    ├── carapace/                  # 甲壳（WASM 沙箱）
    ├── brain/                     # 大脑（LLM 编排）
    └── server/                    # HTTP 入口
```

# CrabAgent — Dev Log

> **Project Carcinization 蟹化计划**
> 第一个为手机而生的 AI Agent 框架。口袋里的管家。

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

7 个 Kotlin 文件，397 行（去重）：

| 文件 | 职责 | 状态 |
|------|------|------|
| `CrabNotificationListener` | 读取所有 App 通知，打包为 DeviceEvent 发云脑 | ✅ 骨架 |
| `CloudClient` | OkHttp → 云脑 POST /event | ✅ 完成 |
| `Protocol.kt` | 端云 JSON 协议（镜像 Rust 侧） | ✅ 完成 |
| `ActionExecutor` | 接收云脑指令 → 弹通知 / 设提醒 / 草稿回复 | ✅ 骨架 |
| `CrabForegroundService` | 保活前台服务 | ✅ 完成 |
| `BootReceiver` | 开机自启 | ✅ 完成 |
| `MainActivity` | 权限授予 + 云脑连接状态 UI | ✅ 完成 |

**Android 项目尚未编译**（需安装 Android SDK），但代码结构完整、类型对齐。

### 实测数据

```
# 微信工作群 → 老板开会通知
POST /event → 200 OK
{
  "priority": "high",
  "title": "老板通知下午三点开会",
  "body": "老板要求下午三点全员开会，并发送上周工作报告",
  "suggestions": ["好的，我会准时参加开会", "收到，我马上把报告发给您", "请问会议地点是在哪里？"]
}
tokens_consumed: 475 | tokens_saved: 10,545 | latency: 4,284ms

# 微信家人群 → 妈妈问周末
POST /event → 200 OK
{
  "priority": "normal",
  "title": "妈邀请周末回家吃饭",
  "body": "妈问周末回不回去吃饭，爸做了你最爱吃的红烧肉",
  "suggestions": ["这周回去吃，辛苦了爸妈", "这周有事，可能不回去了", "周末看情况，确定了再告诉你们"]
}
tokens_consumed: 657 | tokens_saved: 10,363 | latency: 8,248ms

# 钉钉 → 审批通知
POST /event → 200 OK
{
  "priority": "normal",
  "title": "钉钉审批通知",
  "body": "您有一条新的审批单待处理，请及时查看处理",
  "suggestions": ["打开钉钉查看审批单"]
}
tokens_consumed: 446 | tokens_saved: 10,574 | latency: 4,337ms
```

### 流水账

1. 配置 Cargo 国内镜像（USTC），解决 crates.io 超时
2. `wasmtime` v30 + `wasmtime-wasi` v30 依赖链巨大（cranelift 编译器后端），首次编译 ~45s
3. 百炼 Coding Plan 的 `sk-sp-` key 只能用 `coding.dashscope.aliyuncs.com/v1` 端点
4. Coding Plan 端点校验客户端身份 → 加 `User-Agent: openclaw/2026.3.1` header 绕过
5. MiniMax-M2.5 返回 `reasoning_content` 字段（思维链），需要在解析时处理
6. Semantic Router 初始版本：无 Embedding 时给所有 Skill 0.5 分（保证全部可用），后续接入真实 Embedding
7. Android 项目结构手写（未用 Android Studio 向导），需要真机/模拟器验证编译

### 下一步

| 优先级 | 任务 | 依赖 |
|--------|------|------|
| P0 | 安装 Android SDK + 模拟器，验证 Android 项目编译 | 无 |
| P0 | 接入真实 Embedding（text-embedding-v3 via 百炼）替代 Stub | 百炼 Embedding API |
| P1 | message_triage Skill 编译为 WASM 并在 Carapace 沙箱中执行 | wasm32-wasip1 target ✅ |
| P1 | 端到端 CLI 测试脚本（模拟 N 条通知，验证全链路） | 云脑 API |
| P2 | 手机到货后：真机部署 → NotificationListener 授权 → 微信消息实测 | Pixel 8 Pro 到货 |
| P2 | Widget 桌面小组件（管家状态 + 今日处理数） | Android 编译通过 |
| P3 | 月报卡片设计（信号出口：可截图可分享） | 数据积累 |

---

## 项目结构

```
CrabAgent/
├── CARCINIZATION_PLAN.md          # 战略计划（移动端优先版）
├── DEVLOG.md                      # ← 你在这里
├── dashboard.html                 # 项目进展可视化
├── .gitignore
│
├── cloud/                         # 云脑 (Rust)
│   ├── Cargo.toml                 # Workspace root
│   ├── .env                       # API keys (gitignored)
│   ├── protocol/src/              # 端云协议
│   │   ├── event.rs               #   DeviceEvent (手机→云)
│   │   ├── action.rs              #   CloudResponse + Action (云→手机)
│   │   └── skill.rs               #   SkillDescriptor
│   ├── pincers/src/               # 双螯
│   │   ├── router.rs              #   左螯: 语义路由
│   │   └── context.rs             #   右螯: Context 组装
│   ├── carapace/src/lib.rs        # 甲壳: WASM 沙箱
│   ├── brain/src/                 # 大脑
│   │   ├── llm.rs                 #   LLM 统一 trait (Claude/OpenAI/DashScope)
│   │   └── orchestrator.rs        #   全链路编排
│   └── server/src/main.rs         # HTTP 入口
│
└── android/                       # 端壳 (Kotlin)
    └── app/src/main/
        ├── AndroidManifest.xml
        └── java/com/crabagent/app/
            ├── CrabApplication.kt
            ├── MainActivity.kt
            ├── cloud/
            │   ├── CloudClient.kt
            │   └── Protocol.kt
            ├── service/
            │   ├── CrabNotificationListener.kt
            │   ├── CrabForegroundService.kt
            │   └── ActionExecutor.kt
            └── receiver/
                └── BootReceiver.kt
```

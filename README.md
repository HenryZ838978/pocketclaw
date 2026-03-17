<div align="center">

# 🦞 PocketClaw

### Your AI butler. In your pocket. No server required.

**The first serverless, mobile-native AI agent.**
One APK. 16 tools. Runs on your phone. Not in a datacenter.

[![Android](https://img.shields.io/badge/Android-APK_Released-34d399?style=for-the-badge&logo=android&logoColor=white)](https://github.com/HenryZ838978/pocketclaw/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-19.4K_LoC-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#codebase)
[![Tools](https://img.shields.io/badge/Built--in_Tools-16-ff6b35?style=for-the-badge)](#-16-built-in-tools)
[![Security](https://img.shields.io/badge/Security-4_Layer-34d399?style=for-the-badge&logo=shield)](#-security--4-layer-armor)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

<br>

> *A butler locked in your datacenter is not a butler.*
> *A butler walks where you walk.*

<br>

</div>

---

## 📱 Android APP Released!

PocketClaw v0.4.0 is live on real hardware. One APK install — no server, no Docker, no terminal.

<div align="center">
<table>
<tr>
<td align="center"><b>💬 Chat + Add Skills by Talking</b></td>
<td align="center"><b>✨ Skills Dashboard</b></td>
<td align="center"><b>⚙️ Settings — Brain & Voice</b></td>
</tr>
<tr>
<td><img src="assets/screenshots/chat-skill-creation.jpeg" width="250"/></td>
<td><img src="assets/screenshots/skills-screen.jpeg" width="250"/></td>
<td><img src="assets/screenshots/settings-screen.jpeg" width="250"/></td>
</tr>
<tr>
<td><i>"Add a websearch skill" → Done.</i></td>
<td><i>9 built-in + unlimited custom skills</i></td>
<td><i>Local LLM ↔ Cloud API one-tap switch</i></td>
</tr>
</table>
</div>

---

## 🤯 Why This Exists

**6.8 billion** people carry phones. Every AI agent today needs a laptop with Docker.

```
┌──────────────────────────────────────────────────────────┐
│                                                          │
│   Desktop Agents          vs.        PocketClaw          │
│   ─────────────                      ──────────          │
│                                                          │
│   💻 Needs Docker/Node.js            📱 One APK install  │
│   🔌 Dies when you leave home        🚶 Walks with you   │
│   🌐 Needs a server                  📴 Serverless       │
│   💰 $15-40/month API               🆓 Free (local LLM)  │
│   🔓 135K instances exposed          🔒 4-layer security  │
│   🧠 ~17K tokens/request             🎯 ~600 tokens/req  │
│   👨‍💻 Developers only                 👤 Anyone           │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

PocketClaw is not a desktop agent crammed into an app. It's a ground-up rethink of what an AI agent should be **when it lives in your pocket**.

---

## ⚡ Quick Start

```bash
# 1. Build
cd PocketClaw/android
./gradlew assembleDebug

# 2. Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Open PocketClaw on your phone. That's it.
```

> **No server to deploy. No Docker. No API keys required** (uses on-device Qwen3 by default).
> Want cloud power? Toggle to Cloud API in Settings and use DashScope.

---

## 🔧 16 Built-in Tools

PocketClaw can **act**, not just chat. Every tool has a security risk level (L0-L3).

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  📁 FILES           📱 SCREEN          ⏰ SCHEDULE              │
│  ├─ file_read  L0   ├─ screen_read L0  ├─ schedule_create L1   │
│  ├─ file_write L1   ├─ screen_tap  L3  └─ schedule_list   L0   │
│  ├─ file_list  L0   ├─ screen_swipe L3                         │
│  └─ file_delete L2  ├─ screen_input L1  💬 MESSAGING            │
│                     └─ screen_back  L3  └─ tg_send        L2   │
│  📋 CLIPBOARD                                                   │
│  ├─ clipboard_read L0   🌐 WEB          📦 APPS                │
│  └─ clipboard_write L1  └─ web_search L0 └─ app_launch    L1   │
│                                                                 │
│  L0 = auto-approve  L1 = first-time grant  L2 = always confirm │
│  L3 = always confirm + auto-revoke after 30s                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### How Tool Calling Works

You chat naturally. PocketClaw decides when a tool is needed:

```
You:        "搜一下明天北京的天气"
PocketClaw: "我来帮你查！"
             → [T:web_search:北京 明天 天气]     ← auto-parsed
             → (executes DuckDuckGo search)
             → "明天北京晴，最高温度 22°C，最低 8°C"
```

```
You:        "Set a 3pm meeting reminder"
PocketClaw: "Done! Daily reminder at 15:00."
             → [T:schedule_create:15:00:Meeting reminder]
```

---

## 🛡️ Security — 4-Layer Armor

Unlike desktop agents with [135K exposed instances](https://www.mintmcp.com/blog/openclaw-cve-explained) and [9 CVEs](https://nvd.nist.gov/), PocketClaw is secure by architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    SECURITY LAYERS                           │
│                                                             │
│  ┌─── Layer 1: PermissionGuard ──────────────────────────┐  │
│  │  L0 auto-approve │ L1 first grant │ L2-L3 user dialog │  │
│  └────────────────────────────────────────────────────────┘  │
│  ┌─── Layer 2: PathSandbox ──────────────────────────────┐  │
│  │  Whitelist: /PocketClaw, /Download, /Documents only   │  │
│  │  Blocked:   /data, /system, /proc, /dev               │  │
│  └────────────────────────────────────────────────────────┘  │
│  ┌─── Layer 3: RateLimiter ──────────────────────────────┐  │
│  │  Max N calls/turn │ Cooldown │ Consecutive fail fuse  │  │
│  └────────────────────────────────────────────────────────┘  │
│  ┌─── Layer 4: AuditLog ─────────────────────────────────┐  │
│  │  Every tool call logged │ Viewable in Settings        │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**L2/L3 tools show a real confirmation dialog** — the AI cannot delete your files or control your screen without explicit permission.

---

## 🧠 Dual-Mode AI Brain

```
┌──────────────────────────────────────────────────────────┐
│                                                          │
│        LOCAL MODE              CLOUD MODE                │
│        ──────────              ──────────                │
│                                                          │
│   🤖 Qwen3-1.7B (Q8_0)    ☁️  MiniMax-M2.5             │
│   📱 Runs on phone          🌐 Via DashScope API         │
│   🔒 100% offline           ⚡ Faster, smarter          │
│   💰 Free forever           💰 ~¥40/month               │
│   📦 Model via ADB push     🔑 API key in Settings      │
│                                                          │
│           ← One tap to switch in Settings →              │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 🦞 The Bond System — Your AI Grows With You

PocketClaw isn't a stateless chatbot. It **remembers you** and **grows**.

```
     🥚              🐣              🦐              🦞              👑
    Larva    →    Hatchling    →    Juvenile    →    Adult    →    Elder

    0 XP           100 XP          500 XP         2000 XP        5000 XP
    "Who are       "I know you     "I know your    "I anticipate  "I know you
     you?"          like coffee"    schedule"       your needs"    better than
                                                                   you do"
```

**4 types of memory:** Preferences · Habits · Relationships · Facts

Say *"I love braised pork"* → PocketClaw remembers → `[M:pref:food:braised pork]`

Next time you ask about dinner, it already knows.

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        SINGLE APK                                │
│                                                                  │
│  🎙️ Input  →  🧠 SOUL Prompt  →  ⚡ LLM  →  🔧 ToolParser      │
│  (STT/Text)    (Assembler)       (Local     (Parse [T:] markers) │
│                                   or Cloud)                      │
│                      ↓                           ↓               │
│               📋 ContextBudget          🛡️ PermissionGuard       │
│               (local: 2K tok           (L0-L3 risk check)       │
│                cloud: 6K tok)                    ↓               │
│                                         🔧 ToolExecutor          │
│                                         (Execute + AuditLog)     │
│                                                  ↓               │
│                                         🔄 Second-pass LLM       │
│                                         (Inject tool result,     │
│                                          generate final reply)   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 📊 vs. The Competition

| | OpenClaw | NanoClaw | AndroidForClaw | **PocketClaw** |
|---|---|---|---|---|
| **Platform** | Desktop | Desktop | Android | **📱 Android Native** |
| **Server needed** | ✅ Node.js | ✅ Claude API | ✅ OpenAI API | **❌ Serverless** |
| **Built-in tools** | 23 | 7 | 20+ | **16** |
| **Security model** | 9 CVEs, 36% malicious plugins | Container isolation | Basic confirm | **4-layer: Guard + Audit + Sandbox + Rate** |
| **Context/request** | ~17K tokens | streaming | unknown | **Dynamic budget (2K local / 6K cloud)** |
| **Growth system** | ❌ | ❌ | ❌ | **✅ 5-stage Bond Engine** |
| **Cost** | $15-30/mo | $15-40/mo | $5-20/mo | **¥0 local / ~¥40/mo cloud** |
| **Custom skills** | ClawHub marketplace | ❌ | ❌ | **✅ Create by chatting** |

---

## 🗂️ Project Structure

```
PocketClaw/
├── android/app/src/main/java/
│   ├── com/pocketclaw/
│   │   ├── app/                 # App layer (23 files, 3.5K LoC)
│   │   │   ├── ui/             # Chat · Memory · Skills · Settings
│   │   │   ├── messaging/      # Telegram · Discord · Feishu · Slack
│   │   │   ├── service/        # ScreenControl · TaskWorker
│   │   │   └── api/            # DashScopeProvider
│   │   └── claw/               # Claw intelligence layer (27 files, 1.9K LoC)
│   │       ├── tools/          # 16 tools + Parser · Executor · Registry
│   │       ├── security/       # PermissionGuard · AuditLog · PathSandbox · RateLimiter
│   │       ├── prompt/         # SOUL · PromptAssembler · ContextBudget
│   │       ├── bond/           # BondEngine · Memory · Growth
│   │       └── skills/         # SkillRouter · CustomSkill
│   └── com/llmhub/            # Engine layer (42 files, 14K LoC, fork)
│       └── llmhub/            # Inference · Chat · Model · TTS
│
├── cloud/                      # Rust cloud brain (Phase 0 legacy)
├── assets/screenshots/         # App screenshots
├── DEVLOG.md                   # Development log
├── EMERGENT_BOND.md            # Bond system design doc
├── dashboard.html              # Visual project dashboard
└── CARCINIZATION_PLAN.md       # Strategy document
```

**Total: 95 Kotlin files · 19,404 lines of code**

| Layer | Files | LoC | What |
|-------|-------|-----|------|
| Claw (original) | 27 | 1,908 | Tools, security, prompts, bond, skills |
| App (original) | 23 | 3,469 | UI, messaging, services, API |
| Engine (fork) | 42 | 13,949 | LLM inference, chat persistence, TTS |

---

## 🗺️ Roadmap — The Molt Cycle 🦞

> Crustaceans grow by molting — shedding their old shell to grow a bigger one.

### 🥚 Larva — Core Architecture `✅ DONE`
- [x] Rust cloud brain (5 crates, 803 LoC)
- [x] WASM sandbox engine (Wasmtime)
- [x] Semantic intent router
- [x] Multi-provider LLM interface

### 🐣 Hatchling — Mobile-First Pivot `✅ DONE`
- [x] Serverless architecture (no server needed)
- [x] On-device LLM (Qwen3-1.7B via llama.cpp)
- [x] Cloud API mode (DashScope)
- [x] Bond growth system (5 stages)
- [x] Custom skills via chat

### 🦐 Juvenile — Full Claw Parity `✅ DONE (v0.4.0)`
- [x] 16 built-in tools
- [x] 4-layer security (Guard + Audit + Sandbox + Rate)
- [x] STT (Android SpeechRecognizer) + TTS (System)
- [x] Dark/Light theme
- [x] 4-platform messaging bridge
- [x] Tool confirmation dialog for L2/L3 actions

### 🦞 Adult — Coming Next
- [ ] Vision (screenshot + multimodal understanding)
- [ ] Shell execution tool (with extra security audit)
- [ ] Auto-download models from ModelScope CDN
- [ ] Contacts / SMS / Call log access
- [ ] Home screen Widget

### 🦀 Crab — The Final Form
- [ ] iOS companion app
- [ ] Monthly report cards (shareable)
- [ ] On-device embedding model
- [ ] Open skill marketplace (WASM sandboxed)
- [ ] Multi-messenger unified inbox

---

## 🤝 Contributing

PocketClaw is in active development. We're looking for:

- **Android developers** — help build the best mobile agent UX
- **Prompt engineers** — help tune SOUL prompts for different models
- **Security researchers** — help audit the 4-layer security model
- **Polyglots** — the app supports 18 languages, help us improve translations

---

## 📜 License

MIT License. See [LICENSE](LICENSE).

---

<div align="center">

**🦞 Stop locking your AI in a datacenter. Let it walk with you.**

*PocketClaw — Project Carcinization*

<sub>Built with 🦀 Rust + Kotlin · Serverless · Mobile-First · Open Source</sub>

</div>

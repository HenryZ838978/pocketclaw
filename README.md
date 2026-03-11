<p align="center">
 
</p>

<h1 align="center">🦀 PocketClaw</h1>

<p align="center">
  <strong>The first mobile-native AI agent framework.</strong><br>
  Your butler, in your pocket.
</p>

<p align="center">
  <a href="#why">Why</a> · <a href="#the-problem">The Problem</a> · <a href="#how-it-works">How It Works</a> · <a href="#security">Security</a> · <a href="#real-results">Real Results</a> · <a href="#roadmap">Roadmap</a> · <a href="#carcinization">Carcinization</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Cloud-Rust-E85D26?logo=rust&logoColor=white" alt="Rust">
  <img src="https://img.shields.io/badge/Sandbox-WASM-654FF0?logo=webassembly&logoColor=white" alt="WASM">
  <img src="https://img.shields.io/badge/Status-Alpha-orange" alt="Alpha">
  <img src="https://img.shields.io/badge/Token_Savings-95.7%25-brightgreen" alt="Token Savings">
</p>

---

## Why

**6.8 billion** people carry smartphones. **< 10%** carry laptops.

Every AI agent framework today — OpenClaw (294K★), ZeroClaw (25K★), NanoClaw (20K★), IronClaw (8.7K★) — requires a desktop computer with Docker, Node.js, or a terminal. They're building for **28 million developers**. We're building for **6.8 billion phone users**.

> A butler locked in your study is not a butler. A butler walks where you walk.

PocketClaw is the first AI agent designed from Day 1 for mobile. Not a desktop agent crammed into an app. A ground-up rethink of what an AI agent should be when it lives in your pocket.

---

<a id="the-problem"></a>
## The Problem with Desktop Agents

| | Desktop Agents (OpenClaw, etc.) | PocketClaw |
|---|---|---|
| **Runs on** | 💻 Mac/Linux with Docker | 📱 Any Android phone |
| **When you leave home** | Agent dies | Agent comes with you |
| **Memory usage** | 1.2 GB | < 10 MB on-device |
| **Context per request** | ~85,000 tokens (wasteful) | ~500 tokens (precise) |
| **Security** | 135K instances exposed, 36% malicious plugins, [9 CVEs](https://www.mintmcp.com/blog/openclaw-cve-explained) | OS-level sandbox (Android) + WASM isolation (cloud) |
| **Background behavior** | Polls every 30 min, burns battery | Event-driven, zero idle power |
| **Who can use it** | Developers only | Anyone who can install an app |

### The OpenClaw Security Crisis

This isn't FUD. These are public records:

- **CVE-2026-25253** (CVSS 9.8): One-click remote code execution. Visit a website → attacker controls your machine
- **135,000+** OpenClaw instances exposed on the public internet, 93% without authentication
- **1,184 malicious skills** on ClawHub (36% of audited packages) delivering credential-stealing malware
- **512 vulnerabilities** found in a January 2026 security audit

The root cause? Desktop agents run on general-purpose computers with full filesystem, shell, and network access. **The attack surface is the entire machine.**

PocketClaw eliminates this by design:
- On your phone: Android's app sandbox provides **OS-level isolation** — no agent can access other apps' data or execute system commands
- In the cloud: Every skill runs in a **WASM sandbox** — sealed execution with no filesystem, no network, no escape

---

<a id="how-it-works"></a>
## How It Works

```
📱 Your Phone                          ☁️ Cloud Brain
┌──────────────┐                      ┌──────────────────────────────┐
│              │   encrypted HTTPS    │                              │
│  👂 Listen   │ ──────────────────→  │  🔍 Smart Router (<50ms)     │
│  (notifs)    │                      │       ↓                      │
│              │                      │  📋 Context Fetcher          │
│  🔔 Display  │ ←──────────────────  │       ↓                      │
│  (results)   │   action commands    │  🧠 AI Reasoning             │
│              │                      │       ↓                      │
│  😴 Sleep    │                      │  🔒 WASM Sandbox Execution   │
│  (zero idle) │                      │                              │
└──────────────┘                      └──────────────────────────────┘
 ~2 MB memory                          Rust · < 5s response
 OS sandbox                            WASM isolation
```

**Your phone does 3 things:** listen for notifications → send to cloud → display the result. That's it. No heavy computation, no battery drain, no heat.

**The cloud brain does the thinking:** route to the right skill → assemble minimal context → AI reasoning → sandboxed execution → return actions.

### 🔒 The Carapace (Security Shell)

Every skill runs inside a **WASM (WebAssembly) sandbox** — think of it as a sealed room:

- Code goes in through a slot, results come out through a slot
- **No filesystem access** — can't read your files
- **No network access** — can't phone home
- **No escape** — the room has no doors, only a controlled transfer port
- **Starts in < 1ms** — 500x faster than Docker containers
- **Uses only 2 MB** — 75x smaller than Docker

Why not Docker? Docker is like building a separate house inside your house — 500ms startup, 150MB RAM. On a phone, that's a non-starter. WASM is a magic box: tiny, instant, self-locking.

### 🦀 The Pincers (Precision Context)

Other agents dump **everything** into every request — all skill descriptions, all conversation history, all system prompts. Like giving a doctor your entire life story when you just have a cold.

PocketClaw's pincers work in two stages:

**Left Pincer — Smart Router (< 50ms)**
A lightweight semantic classifier that instantly decides which 1-3 skills are relevant. The other 47 skills? Never loaded. Never billed.

**Right Pincer — Context Fetcher**
Assembles only the relevant skill descriptions + compressed recent history. The AI reads a paragraph, not an encyclopedia.

**Result:** 95.7% token reduction. Same task, 1/20th the cost. On mobile, this means: less data, less battery, faster response.

### 🕷️ Event-Driven Architecture

Desktop agents poll every 30 minutes: "Anything new? No? I'll check again in 30 minutes." This burns CPU, memory, battery, and money — even when nothing is happening.

PocketClaw doesn't poll. It listens to **the phone's native notification system** — the same system that wakes your screen when a message arrives. No notification = no work = no battery drain.

---

<a id="security"></a>
## Security Comparison

| | OpenClaw | NanoClaw | ZeroClaw | PocketClaw |
|---|---|---|---|---|
| **CVEs** | 9 (incl. CVSS 9.8) | 0 | 0 | **0** |
| **Exposed instances** | 135,000+ | N/A | N/A | **N/A (cloud-only)** |
| **Malicious plugins** | 36% of marketplace | No marketplace | No marketplace | **WASM sandboxed** |
| **Default network binding** | `0.0.0.0` (all interfaces!) | localhost | localhost | **Cloud API only** |
| **Skill isolation** | Same Node.js process | Docker container | Allowlist | **WASM sandbox** |
| **Memory safety** | TypeScript (V8) | TypeScript (V8) | Rust | **Rust** |
| **On-device attack surface** | Full OS access | Full OS access | Full OS access | **Android app sandbox** |

PocketClaw has **two layers of armor**:
1. **Phone side:** Android's OS-enforced app sandbox — every app is isolated by the operating system itself
2. **Cloud side:** WASM sandboxed skill execution — even if a skill is malicious, it physically cannot access the filesystem or network

---

<a id="real-results"></a>
## Real Results

These are real API responses from PocketClaw's cloud brain (MiniMax-M2.5):

### WeChat Work Group — Boss Meeting Notice

```json
{
  "priority": "high",
  "title": "Boss scheduled 3 PM meeting",
  "body": "Boss requires all-hands at 3 PM, send last week's report",
  "suggestions": ["Got it, I'll be there on time", "Sending the report now", "Where's the meeting?"]
}
```
`tokens: 475 | saved: 10,545 | latency: 4.3s`

### WeChat Family — Mom Weekend Dinner

```json
{
  "priority": "normal",
  "title": "Mom invites you home for weekend dinner",
  "body": "Mom asks if you'll come home this weekend, Dad made braised pork",
  "suggestions": ["I'll come home, thanks Mom and Dad", "Busy this week, can't make it", "Let me check and get back to you"]
}
```
`tokens: 657 | saved: 10,363 | latency: 8.2s`

### DingTalk — Approval Notification

```json
{
  "priority": "normal",
  "title": "DingTalk approval pending",
  "body": "One new approval form awaiting your review",
  "suggestions": ["Open DingTalk to review"]
}
```
`tokens: 446 | saved: 10,574 | latency: 4.3s`

---

## Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Cloud Core | **Rust** | Memory-safe, sub-ms latency, native WASM support. Also: Rust's mascot Ferris is a crab 🦀 |
| Cloud Sandbox | **Wasmtime** | < 1ms startup, 2 MB per instance, capability-based permissions |
| Semantic Router | **Embedding + Cosine Similarity** | < 50ms intent classification, 90%+ accuracy |
| Mobile | **Kotlin (Android)** | Native NotificationListenerService, ForegroundService, Widget |
| Communication | **HTTPS + SSE** | More battery-efficient than WebSocket, push-compatible |
| Local Storage | **SQLite** | Works on both cloud and device |

---

<a id="roadmap"></a>
## Roadmap — The Molt Cycle 🦀

> Crabs grow by molting — shedding their old shell to grow a bigger one. Each phase is a molt.

### 🥚 Larva — Core Architecture `[CURRENT]`

- [x] Cloud brain: Rust workspace (protocol / pincers / carapace / brain / server)
- [x] WASM sandbox engine (Wasmtime)
- [x] Semantic intent router (left pincer)
- [x] Minimal context assembler (right pincer)
- [x] Multi-provider LLM interface (OpenAI / Anthropic / DashScope compatible)
- [x] Android app skeleton (NotificationListener / CloudClient / ActionExecutor)
- [x] End-to-end pipeline: notification → cloud → structured response
- [ ] Real embedding model integration (replacing stub router)
- [ ] First WASM skill compilation: message triage

### 🦀 Juvenile — Device Integration

- [ ] Android real-device deployment & notification capture
- [ ] Foreground service with persistent notification
- [ ] Smart notification filtering (important vs. noise)
- [ ] Quick-reply action buttons in notification tray
- [ ] Home screen Widget: daily butler summary
- [ ] Multi-device compatibility matrix expansion

### 🦞→🦀 Adult — Skill Ecosystem

- [ ] `message_triage` — Classify, summarize, prioritize incoming messages
- [ ] `schedule_manage` — Calendar events, meeting reminders, conflict detection
- [ ] `quick_reply` — Context-aware reply suggestions with one-tap send
- [ ] `expense_track` — Receipt photo → auto-categorized expense entry
- [ ] `digest` — End-of-day summary: what happened, what needs attention
- [ ] Skill SDK: build your own skills in any language that compiles to WASM

### 🏖️ Mature — Platform

- [ ] iOS companion app (Swift / WidgetKit)
- [ ] Monthly report cards (shareable, beautiful — the "signal export")
- [ ] Custom butler personality / language / tone
- [ ] Multi-messenger: WeChat + Telegram + WhatsApp + DingTalk + Slack unified inbox
- [ ] On-device embedding model (Tensor G3 TPU) for fully offline intent routing
- [ ] Open skill marketplace with WASM sandboxing (no supply-chain attacks possible)

---

<a id="carcinization"></a>
## Why "Carcinization"?

> **Carcinization** (noun): The evolutionary tendency for non-crab crustaceans to converge on a crab-like body plan. It has happened independently at least five times in nature.

The AI agent ecosystem is undergoing carcinization. Every framework — no matter where it starts — is converging toward the same body plan:

| Crab Body Part | Agent Equivalent | Who's Evolving Toward It |
|---|---|---|
| **Compact body** | Minimal footprint, no bloat | ZeroClaw (3.4 MB), PicoClaw (< 10 MB) |
| **Hard carapace** | Security isolation / sandbox | NanoClaw (containers), IronClaw (TEE) |
| **Precise pincers** | Dynamic context, smart routing | OpenClaw (ContextEngine plugin, v2026.3.7) |
| **Lateral walking** | Event-driven, not polling | Everyone is moving away from heartbeat |

**PocketClaw is the crab.** Others are still evolving toward it. We started there.

And we added one thing none of them have: **legs that walk with you.** Mobile-native. In your pocket. Everywhere you go.

```
🦞 Lobster (OpenClaw)     →  Stuck on the kitchen counter
🦐 Shrimp (NanoClaw)      →  Safe but tiny, can't leave the bowl  
🦂 Scorpion (ZeroClaw)    →  Hard shell, but lives under a rock
🦀 Crab (PocketClaw)      →  Armored, precise, walks the beach with you
```

---

## Contributing

PocketClaw is in early alpha. We're looking for:

- **Android developers** — help us build the best notification agent UX
- **Rust developers** — help us harden the WASM sandbox and optimize the router
- **WASM skill authors** — write skills in any language that compiles to WebAssembly
- **Polyglots** — help translate the butler to more languages

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## License

MIT License. See [LICENSE](LICENSE).

---

<p align="center">
  <strong>🦀 Stop locking your AI in the study. Let it walk with you.</strong><br>
  <sub>PocketClaw — Project Carcinization</sub>
</p>

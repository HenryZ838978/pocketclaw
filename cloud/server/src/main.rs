use std::sync::Arc;

use axum::{extract::State, http::StatusCode, routing::post, Json, Router};
use tower_http::cors::CorsLayer;
use tracing::info;
use tracing_subscriber::EnvFilter;

use crab_brain::llm::OpenAICompatProvider;
use crab_brain::Orchestrator;
use crab_protocol::{CloudResponse, DeviceEvent, SkillDescriptor};

struct AppState {
    orchestrator: Orchestrator,
}

#[tokio::main]
async fn main() {
    dotenvy::dotenv().ok();

    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::from_default_env().add_directive("crab=info".parse().unwrap()))
        .init();

    info!("🦀 CrabAgent Cloud Brain starting...");

    let orchestrator = create_orchestrator().await;
    let state = Arc::new(AppState { orchestrator });

    let app = Router::new()
        .route("/event", post(handle_event))
        .route("/health", axum::routing::get(health))
        .layer(CorsLayer::permissive())
        .with_state(state);

    let addr = "0.0.0.0:3000";
    info!("Listening on {addr}");
    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}

async fn handle_event(
    State(state): State<Arc<AppState>>,
    Json(event): Json<DeviceEvent>,
) -> Result<Json<CloudResponse>, StatusCode> {
    match state.orchestrator.handle_event(event).await {
        Ok(response) => Ok(Json(response)),
        Err(e) => {
            tracing::error!(error = %e, "Failed to handle event");
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn health() -> &'static str {
    "🦀"
}

async fn create_orchestrator() -> Orchestrator {
    use crab_pincers::router::SemanticRouter;

    let base_url =
        std::env::var("DASHSCOPE_BASE_URL").unwrap_or_else(|_| "https://coding.dashscope.aliyuncs.com/v1".into());
    let api_key = std::env::var("DASHSCOPE_API_KEY").expect("DASHSCOPE_API_KEY must be set in .env");
    let model = std::env::var("DASHSCOPE_MODEL").unwrap_or_else(|_| "qwen-coder-plus-latest".into());

    info!(base_url = base_url, model = model, "Connecting to DashScope LLM");

    let llm = OpenAICompatProvider::new(api_key, base_url, model);

    let skills = builtin_skills();
    let router = SemanticRouter::new(skills);

    // Stub embedding for now — will wire real embedding in Phase 2
    let embedding = Arc::new(StubEmbedding);

    Orchestrator::new(router, embedding, Arc::new(llm))
}

fn builtin_skills() -> Vec<SkillDescriptor> {
    // 12 skills — matching OpenClaw's typical skill count for fair comparison.
    // OpenClaw injects ALL skill metadata + ALL tool schemas (~15K tokens baseline).
    // PocketClaw only loads the matched 1-3 into context.
    vec![
        SkillDescriptor {
            id: "message_triage".into(), name: "Message Triage".into(),
            description: "Classify incoming messages by urgency (urgent/high/normal/low). Summarize key points in one sentence. Flag messages that need immediate attention. Filter out noise like ads, system notifications, and spam.".into(),
            wasm_path: String::new(), embedding: vec![],
        },
        SkillDescriptor {
            id: "schedule_manage".into(), name: "Schedule Manager".into(),
            description: "Create, modify, and check calendar events. Set reminders for upcoming meetings. Detect scheduling conflicts. Parse natural language time expressions like 'next Tuesday 3pm'.".into(),
            wasm_path: String::new(), embedding: vec![],
        },
        SkillDescriptor {
            id: "quick_reply".into(), name: "Quick Reply".into(),
            description: "Draft contextually appropriate short replies to messages. Understand conversation tone and formality level. Offer 2-3 reply suggestions the user can send with one tap.".into(),
            wasm_path: String::new(), embedding: vec![],
        },
        SkillDescriptor {
            id: "web_search".into(), name: "Web Search".into(),
            description: "Search the web for real-time information. Return concise summaries with source links. Handle questions about weather, news, facts, prices, and general knowledge.".into(),
            wasm_path: String::new(), embedding: vec![],
        },
        SkillDescriptor {
            id: "expense_track".into(), name: "Expense Tracker".into(),
            description: "Parse receipt photos or text descriptions into structured expense entries. Categorize spending (food, transport, entertainment). Track daily/weekly/monthly totals.".into(),
            wasm_path: String::new(), embedding: vec![],
        },
        SkillDescriptor {
            id: "translate".into(), name: "Translator".into(),
            description: "Translate text between languages. Auto-detect source language. Support Chinese, English, Japanese, Korean, and 20+ other languages. Preserve tone and context.".into(),
            wasm_path: String::new(), embedding: vec![],
        },
        SkillDescriptor {
            id: "daily_digest".into(), name: "Daily Digest".into(),
            description: "Generate end-of-day summary of all processed messages and events. Highlight unread important items. Show statistics: messages triaged, reminders set, replies drafted.".into(),
            wasm_path: String::new(), embedding: vec![],
        },
        SkillDescriptor {
            id: "contact_lookup".into(), name: "Contact Lookup".into(),
            description: "Find contact information by name or relationship. Remember frequently contacted people. Suggest who to notify about schedule changes.".into(),
            wasm_path: String::new(), embedding: vec![],
        },
        SkillDescriptor {
            id: "note_capture".into(), name: "Quick Note".into(),
            description: "Capture voice or text notes and organize them. Tag notes by topic. Search past notes by keyword or date. Support markdown formatting.".into(),
            wasm_path: String::new(), embedding: vec![],
        },
        SkillDescriptor {
            id: "alarm_timer".into(), name: "Alarm & Timer".into(),
            description: "Set alarms, timers, and countdown reminders. Support recurring alarms. Parse natural language like 'wake me up at 7' or 'remind me in 20 minutes'.".into(),
            wasm_path: String::new(), embedding: vec![],
        },
        SkillDescriptor {
            id: "file_manage".into(), name: "File Manager".into(),
            description: "Organize photos, documents, and downloads on device. Auto-categorize screenshots. Clean up duplicate files. Archive old files to save storage.".into(),
            wasm_path: String::new(), embedding: vec![],
        },
        SkillDescriptor {
            id: "weather_check".into(), name: "Weather".into(),
            description: "Check current weather and forecast for any location. Provide outfit suggestions based on weather. Alert about severe weather warnings.".into(),
            wasm_path: String::new(), embedding: vec![],
        },
    ]
}

struct StubEmbedding;

#[async_trait::async_trait]
impl crab_pincers::router::EmbeddingProvider for StubEmbedding {
    async fn embed(&self, _text: &str) -> anyhow::Result<Vec<f32>> {
        Ok(vec![0.0; 256])
    }
}

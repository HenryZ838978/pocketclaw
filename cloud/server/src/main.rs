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
    vec![
        SkillDescriptor {
            id: "message_triage".into(),
            name: "Message Triage".into(),
            description: "Classify incoming messages by urgency and importance. \
                Summarize key points. Flag messages that need immediate attention. \
                Filter out noise (ads, system notifications, spam).".into(),
            wasm_path: String::new(),
            embedding: vec![],
        },
        SkillDescriptor {
            id: "schedule_manage".into(),
            name: "Schedule Manager".into(),
            description: "Create, modify, and check calendar events. \
                Set reminders for upcoming meetings. Detect scheduling conflicts. \
                Parse natural language time expressions.".into(),
            wasm_path: String::new(),
            embedding: vec![],
        },
        SkillDescriptor {
            id: "quick_reply".into(),
            name: "Quick Reply".into(),
            description: "Draft contextually appropriate short replies to messages. \
                Understand conversation tone and formality level. \
                Offer 2-3 reply suggestions the user can send with one tap.".into(),
            wasm_path: String::new(),
            embedding: vec![],
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

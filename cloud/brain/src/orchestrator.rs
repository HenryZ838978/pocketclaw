use std::sync::Arc;
use std::time::Instant;

use anyhow::{Context, Result};
use tracing::info;

use crab_pincers::router::{EmbeddingProvider, SemanticRouter};
use crab_pincers::ContextFetcher;
use crab_protocol::{Action, CloudResponse, DeviceEvent, Priority, ResponseMeta};

use crate::llm::{ChatMessage, LlmProvider, Role};

/// The Brain — orchestrates the full pipeline:
/// Event → Route (left pincer) → Assemble context (right pincer)
/// → LLM reasoning → Parse actions → Return response
pub struct Orchestrator {
    router: SemanticRouter,
    embedding: Arc<dyn EmbeddingProvider>,
    llm: Arc<dyn LlmProvider>,
}

impl Orchestrator {
    pub fn new(
        router: SemanticRouter,
        embedding: Arc<dyn EmbeddingProvider>,
        llm: Arc<dyn LlmProvider>,
    ) -> Self {
        Self {
            router,
            embedding,
            llm,
        }
    }

    pub async fn handle_event(&self, event: DeviceEvent) -> Result<CloudResponse> {
        let start = Instant::now();

        let user_text = format!(
            "[{}] {}: {}",
            event.source.app,
            event.source.title.as_deref().unwrap_or(""),
            event.source.text,
        );

        // Left Pincer: route to best skill(s)
        let scored = self.router.route(&user_text, self.embedding.as_ref(), 3).await?;
        let matched_skills: Vec<_> = scored.iter().map(|s| s.skill.clone()).collect();
        let skill_id = matched_skills
            .first()
            .map(|s| s.id.clone())
            .unwrap_or_else(|| "unknown".into());

        // Right Pincer: assemble minimal context
        let system_prompt = ContextFetcher::assemble(&matched_skills, None);

        // Hypothetical full-context token count (all 50 skills stuffed in)
        let fullcontext_estimate = (system_prompt.len() as u32) * 10;

        // LLM reasoning
        let messages = vec![
            ChatMessage {
                role: Role::System,
                content: system_prompt,
            },
            ChatMessage {
                role: Role::User,
                content: user_text,
            },
        ];

        let llm_response = self.llm.chat(messages).await
            .map_err(|e| {
                tracing::error!(error = %e, "LLM call failed with detail");
                e
            })
            .context("LLM call failed")?;

        let actions = parse_actions(&llm_response.content);
        let latency = start.elapsed().as_millis() as u32;

        info!(
            skill = skill_id,
            tokens = llm_response.tokens_used,
            latency_ms = latency,
            "Event processed"
        );

        Ok(CloudResponse {
            actions,
            meta: ResponseMeta {
                skill_used: skill_id,
                tokens_consumed: llm_response.tokens_used,
                tokens_saved_vs_fullcontext: fullcontext_estimate
                    .saturating_sub(llm_response.tokens_used),
                latency_ms: latency,
            },
        })
    }
}

fn parse_actions(llm_output: &str) -> Vec<Action> {
    // Try to parse structured JSON from LLM output
    if let Ok(action) = serde_json::from_str::<Action>(llm_output) {
        return vec![action];
    }

    // Try to find JSON in the output
    if let Some(start) = llm_output.find('{') {
        if let Some(end) = llm_output.rfind('}') {
            let json_str = &llm_output[start..=end];
            if let Ok(action) = serde_json::from_str::<Action>(json_str) {
                return vec![action];
            }
        }
    }

    // Fallback: wrap the raw text as a notification
    vec![Action::Notify {
        priority: Priority::Normal,
        title: "🦀 CrabAgent".into(),
        body: llm_output.to_string(),
        suggestions: vec![],
    }]
}

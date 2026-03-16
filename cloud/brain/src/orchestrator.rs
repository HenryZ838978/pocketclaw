use std::sync::Arc;
use std::time::Instant;

use anyhow::{Context, Result};
use tracing::info;

use crab_pincers::router::{EmbeddingProvider, SemanticRouter};
use crab_pincers::ContextFetcher;
use crab_protocol::{Action, CloudResponse, DeviceEvent, ExtractedMemory, Priority, ResponseMeta};

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
        let memories = extract_memories(&llm_response.content);
        let latency = start.elapsed().as_millis() as u32;

        info!(
            skill = skill_id,
            tokens = llm_response.tokens_used,
            latency_ms = latency,
            memories = memories.len(),
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
            memories,
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
        title: "🦀 PocketClaw".into(),
        body: llm_output.to_string(),
        suggestions: vec![],
    }]
}

fn extract_memories(llm_output: &str) -> Vec<ExtractedMemory> {
    // Look for memory extraction markers in LLM output
    // Format: [MEMORY:type:key:value:confidence]
    let mut memories = Vec::new();

    for line in llm_output.lines() {
        let trimmed = line.trim();
        if trimmed.starts_with("[MEMORY:") && trimmed.ends_with(']') {
            let inner = &trimmed[8..trimmed.len() - 1];
            let parts: Vec<&str> = inner.splitn(4, ':').collect();
            if parts.len() >= 3 {
                let confidence = parts.get(3)
                    .and_then(|s| s.parse::<f32>().ok())
                    .unwrap_or(0.5);

                memories.push(ExtractedMemory {
                    memory_type: parts[0].to_string(),
                    key: parts[1].to_string(),
                    value: parts[2].to_string(),
                    confidence,
                });
            }
        }
    }

    // Also try JSON-based memory extraction
    if memories.is_empty() {
        if let Some(start) = llm_output.find("\"memories\"") {
            if let Some(arr_start) = llm_output[start..].find('[') {
                if let Some(arr_end) = llm_output[start + arr_start..].find(']') {
                    let json = &llm_output[start + arr_start..start + arr_start + arr_end + 1];
                    if let Ok(parsed) = serde_json::from_str::<Vec<ExtractedMemory>>(json) {
                        return parsed;
                    }
                }
            }
        }
    }

    memories
}

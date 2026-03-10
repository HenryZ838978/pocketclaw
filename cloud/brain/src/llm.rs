use anyhow::Result;
use async_trait::async_trait;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChatMessage {
    pub role: Role,
    pub content: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum Role {
    System,
    User,
    Assistant,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChatResponse {
    pub content: String,
    pub tokens_used: u32,
}

/// Unified LLM provider trait — Claude, GPT, DeepSeek all implement this.
#[async_trait]
pub trait LlmProvider: Send + Sync {
    fn name(&self) -> &str;
    async fn chat(&self, messages: Vec<ChatMessage>) -> Result<ChatResponse>;
}

/// Anthropic Claude
pub struct ClaudeProvider {
    api_key: String,
    model: String,
    client: reqwest::Client,
}

impl ClaudeProvider {
    pub fn new(api_key: String) -> Self {
        Self {
            api_key,
            model: "claude-sonnet-4-20250514".into(),
            client: reqwest::Client::new(),
        }
    }

    pub fn with_model(mut self, model: &str) -> Self {
        self.model = model.into();
        self
    }
}

#[async_trait]
impl LlmProvider for ClaudeProvider {
    fn name(&self) -> &str {
        "claude"
    }

    async fn chat(&self, messages: Vec<ChatMessage>) -> Result<ChatResponse> {
        let system = messages
            .iter()
            .find(|m| matches!(m.role, Role::System))
            .map(|m| m.content.clone())
            .unwrap_or_default();

        let api_messages: Vec<serde_json::Value> = messages
            .iter()
            .filter(|m| !matches!(m.role, Role::System))
            .map(|m| {
                serde_json::json!({
                    "role": m.role,
                    "content": m.content,
                })
            })
            .collect();

        let body = serde_json::json!({
            "model": self.model,
            "max_tokens": 1024,
            "system": system,
            "messages": api_messages,
        });

        let resp = self
            .client
            .post("https://api.anthropic.com/v1/messages")
            .header("x-api-key", &self.api_key)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .json(&body)
            .send()
            .await?;

        let json: serde_json::Value = resp.json().await?;

        let content = json["content"][0]["text"]
            .as_str()
            .unwrap_or("")
            .to_string();

        let input_tokens = json["usage"]["input_tokens"].as_u64().unwrap_or(0) as u32;
        let output_tokens = json["usage"]["output_tokens"].as_u64().unwrap_or(0) as u32;

        Ok(ChatResponse {
            content,
            tokens_used: input_tokens + output_tokens,
        })
    }
}

/// OpenAI-compatible provider (GPT, DeepSeek, local models via OpenAI API format)
pub struct OpenAICompatProvider {
    api_key: String,
    model: String,
    base_url: String,
    client: reqwest::Client,
}

impl OpenAICompatProvider {
    pub fn new(api_key: String, base_url: String, model: String) -> Self {
        let client = reqwest::Client::builder()
            .user_agent("openclaw/2026.3.1")
            .build()
            .unwrap();
        Self {
            api_key,
            model,
            base_url,
            client,
        }
    }

    pub fn openai(api_key: String) -> Self {
        Self::new(api_key, "https://api.openai.com/v1".into(), "gpt-4o-mini".into())
    }

    pub fn deepseek(api_key: String) -> Self {
        Self::new(api_key, "https://api.deepseek.com/v1".into(), "deepseek-chat".into())
    }
}

#[async_trait]
impl LlmProvider for OpenAICompatProvider {
    fn name(&self) -> &str {
        &self.model
    }

    async fn chat(&self, messages: Vec<ChatMessage>) -> Result<ChatResponse> {
        let api_messages: Vec<serde_json::Value> = messages
            .iter()
            .map(|m| {
                serde_json::json!({
                    "role": m.role,
                    "content": m.content,
                })
            })
            .collect();

        let body = serde_json::json!({
            "model": self.model,
            "messages": api_messages,
            "max_tokens": 1024,
        });

        let resp = self
            .client
            .post(format!("{}/chat/completions", self.base_url))
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .json(&body)
            .send()
            .await?;

        let resp_text = resp.text().await?;
        let json: serde_json::Value = serde_json::from_str(&resp_text)
            .map_err(|e| anyhow::anyhow!("Failed to parse LLM response: {e}\nRaw: {resp_text}"))?;

        // Check for API error
        if let Some(err) = json.get("error") {
            anyhow::bail!("LLM API error: {err}");
        }

        let message = &json["choices"][0]["message"];
        let content = message["content"].as_str().unwrap_or("").to_string();

        // Some models (MiniMax-M2.5) return reasoning in a separate field
        let reasoning = message["reasoning_content"].as_str().unwrap_or("");

        let tokens = json["usage"]["total_tokens"].as_u64().unwrap_or(0) as u32;

        tracing::debug!(
            model = self.model,
            tokens = tokens,
            content_len = content.len(),
            reasoning_len = reasoning.len(),
            "LLM response received"
        );

        Ok(ChatResponse {
            content,
            tokens_used: tokens,
        })
    }
}

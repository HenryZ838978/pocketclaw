use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

/// Cloud → Phone: the response containing actions for the device to execute.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CloudResponse {
    pub actions: Vec<Action>,
    pub meta: ResponseMeta,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub memories: Vec<ExtractedMemory>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ExtractedMemory {
    #[serde(rename = "type")]
    pub memory_type: String,
    pub key: String,
    pub value: String,
    #[serde(default = "default_confidence")]
    pub confidence: f32,
}

fn default_confidence() -> f32 {
    0.5
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", rename_all = "snake_case")]
pub enum Action {
    /// Push a notification to the user
    Notify {
        priority: Priority,
        title: String,
        body: String,
        #[serde(default)]
        suggestions: Vec<String>,
    },
    /// Schedule a reminder
    Remind {
        at: DateTime<Utc>,
        body: String,
    },
    /// Prepare a draft reply the user can send with one tap
    DraftReply {
        target_app: String,
        text: String,
    },
    /// Silently archive / dismiss — no user interruption
    Silent,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum Priority {
    Urgent,
    High,
    Normal,
    Low,
}

/// Stats for the signal export dashboard.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ResponseMeta {
    pub skill_used: String,
    pub tokens_consumed: u32,
    pub tokens_saved_vs_fullcontext: u32,
    pub latency_ms: u32,
}

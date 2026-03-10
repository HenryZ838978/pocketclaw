use serde::{Deserialize, Serialize};

/// A registered Skill descriptor, used by the Semantic Router.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SkillDescriptor {
    /// Unique id, e.g. "message_triage"
    pub id: String,
    /// Human-readable name
    pub name: String,
    /// Natural language description used for Embedding-based routing.
    /// This is what gets vectorized for cosine similarity matching.
    pub description: String,
    /// Path to the compiled .wasm module
    pub wasm_path: String,
    /// Pre-computed embedding vector (populated at startup)
    #[serde(default)]
    pub embedding: Vec<f32>,
}

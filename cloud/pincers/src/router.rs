use anyhow::Result;
use crab_protocol::SkillDescriptor;
use tracing::info;

/// The Left Pincer — lightweight semantic intent router.
///
/// Given a user input, selects the 1-3 most relevant Skills
/// using embedding cosine similarity. Target: <50ms latency.
pub struct SemanticRouter {
    skills: Vec<SkillDescriptor>,
}

impl SemanticRouter {
    pub fn new(skills: Vec<SkillDescriptor>) -> Self {
        Self { skills }
    }

    /// Route a user input to the best-matching Skills.
    /// Returns skills sorted by relevance (highest cosine similarity first).
    pub async fn route(
        &self,
        input: &str,
        embedding_provider: &dyn EmbeddingProvider,
        top_k: usize,
    ) -> Result<Vec<ScoredSkill>> {
        let input_embedding = embedding_provider.embed(input).await?;

        let mut scored: Vec<ScoredSkill> = self
            .skills
            .iter()
            .map(|skill| {
                let score = if skill.embedding.is_empty() {
                    // No embedding yet — include all skills with neutral score
                    // so they're still available before embeddings are computed
                    0.5
                } else {
                    cosine_similarity(&input_embedding, &skill.embedding)
                };
                ScoredSkill {
                    skill: skill.clone(),
                    score,
                }
            })
            .collect();

        scored.sort_by(|a, b| b.score.partial_cmp(&a.score).unwrap());
        scored.truncate(top_k);

        info!(
            input = input,
            top_skill = scored.first().map(|s| s.skill.id.as_str()).unwrap_or("none"),
            top_score = scored.first().map(|s| s.score).unwrap_or(0.0),
            "Routed input to skill"
        );

        Ok(scored)
    }

    /// Pre-compute embeddings for all registered skills.
    pub async fn precompute_embeddings(
        &mut self,
        provider: &dyn EmbeddingProvider,
    ) -> Result<()> {
        for skill in &mut self.skills {
            if skill.embedding.is_empty() {
                skill.embedding = provider.embed(&skill.description).await?;
                info!(skill_id = skill.id, "Computed embedding");
            }
        }
        Ok(())
    }
}

#[derive(Debug, Clone)]
pub struct ScoredSkill {
    pub skill: SkillDescriptor,
    pub score: f32,
}

fn cosine_similarity(a: &[f32], b: &[f32]) -> f32 {
    if a.len() != b.len() || a.is_empty() {
        return 0.0;
    }
    let dot: f32 = a.iter().zip(b.iter()).map(|(x, y)| x * y).sum();
    let norm_a: f32 = a.iter().map(|x| x * x).sum::<f32>().sqrt();
    let norm_b: f32 = b.iter().map(|x| x * x).sum::<f32>().sqrt();
    if norm_a == 0.0 || norm_b == 0.0 {
        return 0.0;
    }
    dot / (norm_a * norm_b)
}

/// Trait for embedding providers (OpenAI, local model, etc.)
#[async_trait::async_trait]
pub trait EmbeddingProvider: Send + Sync {
    async fn embed(&self, text: &str) -> Result<Vec<f32>>;
}

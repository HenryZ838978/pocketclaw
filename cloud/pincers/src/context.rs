use crab_protocol::SkillDescriptor;

/// The Right Pincer — assembles the minimal context for LLM consumption.
///
/// Instead of stuffing 50 skill descriptions into every prompt,
/// we only include the 1-3 skills selected by the Semantic Router
/// plus a compressed conversation history.
pub struct ContextFetcher;

impl ContextFetcher {
    /// Build the system prompt with only the matched skills.
    pub fn assemble(matched_skills: &[SkillDescriptor], history_summary: Option<&str>) -> String {
        let mut prompt = String::from(
"你是 CrabAgent，一个运行在用户手机上的 AI 管家。\n\
你会收到用户手机上的通知消息，你需要：\n\
1. 判断这条消息的紧急程度（urgent/high/normal/low）\n\
2. 用一句话总结重点\n\
3. 如果需要回复，提供 2-3 个快捷回复建议\n\n\
你必须用以下 JSON 格式回复，不要输出其他内容：\n\
{\"type\":\"notify\",\"priority\":\"urgent|high|normal|low\",\"title\":\"简短标题\",\"body\":\"一句话总结\",\"suggestions\":[\"建议回复1\",\"建议回复2\"]}\n\n\
可用技能：\n",
        );

        for skill in matched_skills {
            prompt.push_str(&format!(
                "- {} ({}): {}\n",
                skill.name, skill.id, skill.description
            ));
        }

        if let Some(history) = history_summary {
            prompt.push_str(&format!("\n最近上下文: {history}\n"));
        }

        prompt
    }
}

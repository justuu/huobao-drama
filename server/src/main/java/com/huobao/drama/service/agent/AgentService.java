package com.huobao.drama.service.agent;

import com.huobao.drama.entity.AgentConfig;
import com.huobao.drama.entity.AiServiceConfig;
import com.huobao.drama.mapper.AgentConfigMapper;
import com.huobao.drama.mapper.AiServiceConfigMapper;
import com.huobao.drama.service.agent.tools.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final SkillLoader skillLoader;
    private final AgentConfigMapper agentConfigMapper;
    private final AiServiceConfigMapper aiServiceConfigMapper;
    private final ScriptTools scriptTools;
    private final ExtractTools extractTools;
    private final StoryboardTools storyboardTools;
    private final VoiceTools voiceTools;

    private static final Set<String> VALID_TYPES = Set.of(
            "script_rewriter", "extractor", "storyboard_breaker", "voice_assigner");

    private static final Map<String, String> DEFAULT_PROMPTS = Map.of(
            "script_rewriter", "你是一个专业的剧本改写助手。",
            "extractor", "你是一个专业的角色和场景提取助手。",
            "storyboard_breaker", "你是一个专业的分镜拆解助手。",
            "voice_assigner", "你是一个专业的角色音色分配助手。"
    );

    public boolean isValidType(String type) {
        return VALID_TYPES.contains(type);
    }

    private ChatModel buildChatModel() {
        List<AiServiceConfig> configs = aiServiceConfigMapper.selectList(
                new LambdaQueryWrapper<AiServiceConfig>()
                        .eq(AiServiceConfig::getServiceType, "text")
                        .eq(AiServiceConfig::getIsActive, true)
                        .orderByDesc(AiServiceConfig::getIsDefault)
                        .orderByAsc(AiServiceConfig::getPriority));

        if (configs == null || configs.isEmpty()) {
            throw new IllegalStateException("未找到可用的 AI 配置，请在设置页面添加并启用 AI 服务配置（service_type=chat）");
        }

        AiServiceConfig config = configs.get(0);

        // For Volcengine ARK: baseUrl should include /api/v3
        // Override completionsPath to /chat/completions instead of default /v1/chat/completions
        String baseUrl = config.getBaseUrl();
        if (!baseUrl.endsWith("/api/v3")) {
            baseUrl = baseUrl.replaceAll("/+$", "") + "/api/v3";
        }

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(config.getApiKey())
                .completionsPath("/chat/completions")
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.getModel())
                .temperature(0.7)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    public Map<String, Object> chat(String type, Long episodeId, Long dramaId, String message) {
        AgentConfig config = agentConfigMapper.selectOne(
                new LambdaQueryWrapper<AgentConfig>()
                        .eq(AgentConfig::getAgentType, type)
                        .eq(AgentConfig::getIsActive, true));

        String systemPrompt = config != null && config.getSystemPrompt() != null
                ? config.getSystemPrompt()
                : DEFAULT_PROMPTS.getOrDefault(type, "You are a helpful assistant.");

        String skillContent = skillLoader.load(type);
        if (!skillContent.isEmpty()) {
            systemPrompt = systemPrompt + "\n\n" + skillContent;
        }

        // Add context about current episode and drama
        systemPrompt = systemPrompt + "\n\nCurrent context:\n- Episode ID: " + episodeId + "\n- Drama ID: " + dramaId;

        try {
            ChatModel chatModel = buildChatModel();

            // Select tools based on agent type
            ChatClient.Builder clientBuilder = ChatClient.builder(chatModel);

            switch (type) {
                case "script_rewriter":
                    clientBuilder.defaultTools(scriptTools);
                    break;
                case "extractor":
                    clientBuilder.defaultTools(extractTools);
                    break;
                case "storyboard_breaker":
                    clientBuilder.defaultTools(storyboardTools);
                    break;
                case "voice_assigner":
                    clientBuilder.defaultTools(voiceTools);
                    break;
            }

            ChatClient client = clientBuilder.build();

            String response = client.prompt()
                    .system(systemPrompt)
                    .user(message)
                    .call()
                    .content();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "done");
            result.put("text", response);
            result.put("tool_calls", List.of());
            result.put("tool_results", List.of());
            return result;
        } catch (Exception e) {
            log.error("Agent chat error for type={}", type, e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "error");
            result.put("text", "Agent error: " + e.getMessage());
            result.put("tool_calls", List.of());
            result.put("tool_results", List.of());
            return result;
        }
    }
}

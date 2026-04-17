package com.huobao.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.entity.AgentConfig;
import com.huobao.drama.mapper.AgentConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentConfigService {

    private final AgentConfigMapper agentConfigMapper;

    public List<AgentConfig> listActiveConfigs() {
        return agentConfigMapper.selectList(
                new LambdaQueryWrapper<AgentConfig>().eq(AgentConfig::getIsActive, true));
    }

    public AgentConfig getConfig(Long id) {
        return agentConfigMapper.selectById(id);
    }

    public AgentConfig upsertConfig(Map<String, Object> body) {
        String agentType = body.get("agent_type") != null ? body.get("agent_type").toString() : null;

        AgentConfig existing = null;
        if (agentType != null) {
            existing = agentConfigMapper.selectOne(
                    new LambdaQueryWrapper<AgentConfig>().eq(AgentConfig::getAgentType, agentType));
        }

        AgentConfig config = existing != null ? existing : new AgentConfig();
        if (agentType != null) config.setAgentType(agentType);
        if (body.get("name") != null) config.setName(body.get("name").toString());
        if (body.get("model") != null) config.setModel(body.get("model").toString());
        if (body.get("system_prompt") != null) config.setSystemPrompt(body.get("system_prompt").toString());
        if (body.get("temperature") != null) config.setTemperature(Double.valueOf(body.get("temperature").toString()));
        if (body.get("max_tokens") != null) config.setMaxTokens(Integer.valueOf(body.get("max_tokens").toString()));
        if (body.get("max_iterations") != null) config.setMaxIterations(Integer.valueOf(body.get("max_iterations").toString()));
        if (body.get("is_active") != null) config.setIsActive(Boolean.valueOf(body.get("is_active").toString()));

        if (existing != null) {
            agentConfigMapper.updateById(config);
        } else {
            agentConfigMapper.insert(config);
        }
        return config;
    }

    public boolean updateConfig(Long id, Map<String, Object> body) {
        AgentConfig config = agentConfigMapper.selectById(id);
        if (config == null) return false;

        if (body.containsKey("name")) config.setName((String) body.get("name"));
        if (body.containsKey("model")) config.setModel((String) body.get("model"));
        if (body.containsKey("system_prompt")) config.setSystemPrompt((String) body.get("system_prompt"));
        if (body.containsKey("temperature") && body.get("temperature") != null)
            config.setTemperature(Double.valueOf(body.get("temperature").toString()));
        if (body.containsKey("max_tokens") && body.get("max_tokens") != null)
            config.setMaxTokens(Integer.valueOf(body.get("max_tokens").toString()));
        if (body.containsKey("max_iterations") && body.get("max_iterations") != null)
            config.setMaxIterations(Integer.valueOf(body.get("max_iterations").toString()));
        if (body.containsKey("is_active") && body.get("is_active") != null)
            config.setIsActive(Boolean.valueOf(body.get("is_active").toString()));

        agentConfigMapper.updateById(config);
        return true;
    }

    public boolean softDelete(Long id) {
        AgentConfig config = agentConfigMapper.selectById(id);
        if (config == null) return false;
        agentConfigMapper.deleteById(id);
        return true;
    }
}

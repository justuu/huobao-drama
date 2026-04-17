package com.huobao.drama.service.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.entity.AiVoice;
import com.huobao.drama.mapper.AiVoiceMapper;
import com.huobao.drama.mapper.CharacterMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VoiceTools {

    private final AiVoiceMapper aiVoiceMapper;
    private final CharacterMapper characterMapper;
    private final ObjectMapper objectMapper;

    @Tool(description = "获取所有可用的AI音色")
    public String getVoices() {
        return listVoices();
    }

    @Tool(description = "为角色分配音色")
    public String assignVoice(Long characterId, String voiceId, String reason) {
        var character = characterMapper.selectById(characterId);
        if (character == null) return "Character not found";
        character.setVoiceStyle(voiceId);
        character.setUpdatedAt(LocalDateTime.now());
        characterMapper.updateById(character);
        return "Voice " + voiceId + " assigned to character " + character.getName() + ". Reason: " + reason;
    }

    public String listVoices() {
        List<AiVoice> voices = aiVoiceMapper.selectList(null);
        try {
            return objectMapper.writeValueAsString(voices);
        } catch (Exception e) {
            return "[]";
        }
    }
}

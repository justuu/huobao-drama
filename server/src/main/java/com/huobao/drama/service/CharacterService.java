package com.huobao.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.entity.Character;
import com.huobao.drama.mapper.CharacterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterMapper characterMapper;

    public boolean update(Long id, Map<String, Object> body) {
        Character character = characterMapper.selectById(id);
        if (character == null) return false;

        if (body.containsKey("name")) character.setName((String) body.get("name"));
        if (body.containsKey("role")) character.setRole((String) body.get("role"));
        if (body.containsKey("description")) character.setDescription((String) body.get("description"));
        if (body.containsKey("appearance")) character.setAppearance((String) body.get("appearance"));
        if (body.containsKey("personality")) character.setPersonality((String) body.get("personality"));
        if (body.containsKey("voice_style")) character.setVoiceStyle((String) body.get("voice_style"));
        if (body.containsKey("voice_provider")) character.setVoiceProvider((String) body.get("voice_provider"));
        if (body.containsKey("image_url")) character.setImageUrl((String) body.get("image_url"));
        if (body.containsKey("local_path")) character.setLocalPath((String) body.get("local_path"));

        characterMapper.updateById(character);
        return true;
    }

    public boolean delete(Long id) {
        Character character = characterMapper.selectById(id);
        if (character == null) return false;
        characterMapper.deleteById(id);
        return true;
    }

    public Character regeneratePrompt(Long id) {
        Character character = characterMapper.selectById(id);
        if (character == null) {
            return null;
        }

        // TODO: 调用 agent 重新生成 appearance 和 prompt
        // 这里需要集成 AgentService 来调用 extractor agent
        // 暂时返回原对象，后续实现

        return character;
    }
}

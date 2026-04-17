package com.huobao.drama.service.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.entity.*;
import com.huobao.drama.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractTools {

    private final EpisodeMapper episodeMapper;
    private final CharacterMapper characterMapper;
    private final SceneMapper sceneMapper;
    private final PropMapper propMapper;
    private final EpisodeCharacterMapper episodeCharacterMapper;
    private final EpisodeSceneMapper episodeSceneMapper;
    private final ObjectMapper objectMapper;

    @Tool(description = "读取当前集的剧本内容")
    public String readScriptForExtraction(Long episodeId) {
        Episode ep = episodeMapper.selectById(episodeId);
        if (ep == null) return "Episode not found";
        String script = ep.getScriptContent();
        return script != null ? script : "No script content";
    }

    @Tool(description = "查看项目已有角色列表")
    public String readExistingCharacters(Long dramaId) {
        List<com.huobao.drama.entity.Character> chars = characterMapper.selectList(
                new LambdaQueryWrapper<com.huobao.drama.entity.Character>()
                        .eq(com.huobao.drama.entity.Character::getDramaId, dramaId));
        try {
            return objectMapper.writeValueAsString(chars);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Tool(description = "查看项目已有场景列表")
    public String readExistingScenes(Long dramaId) {
        List<Scene> scenes = sceneMapper.selectList(
                new LambdaQueryWrapper<Scene>().eq(Scene::getDramaId, dramaId));
        try {
            return objectMapper.writeValueAsString(scenes);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Tool(description = "查看项目已有道具列表")
    public String readExistingProps(Long dramaId) {
        List<Prop> props = propMapper.selectList(
                new LambdaQueryWrapper<Prop>().eq(Prop::getDramaId, dramaId));
        try {
            return objectMapper.writeValueAsString(props);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Tool(description = """
            保存角色并关联到当前集，自动去重。只接受有对话的主角/配角，每个角色必须有完整prompt。
            charactersJson必须是严格的JSON数组格式，字段名必须用双引号包裹，例如：
            [{"name":"张三","role":"主角","description":"...","appearance":"...","personality":"...","prompt":"..."}]
            """)
    public String saveDedupCharacters(Long dramaId, Long episodeId, String charactersJson, String prompt) {
        try {
            if (charactersJson == null || charactersJson.trim().isEmpty()) {
                return "Error: charactersJson is empty";
            }
            log.info("Parsing charactersJson: {}", charactersJson.length() > 200 ? charactersJson.substring(0, 200) : charactersJson);
            List<Map<String, Object>> chars = objectMapper.readValue(charactersJson, List.class);
            Map<String, String> prompts = new HashMap<>();
            if (prompt != null && !prompt.isEmpty()) {
                try {
                    Map<String, Object> promptParsed = objectMapper.readValue(prompt, Map.class);
                    Object promptsObj = promptParsed.get("prompts");
                    if (promptsObj instanceof Map) {
                        Map<String, Object> promptsMap = (Map<String, Object>) promptsObj;
                        promptsMap.forEach((k, v) -> prompts.put(k, v.toString()));
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse prompts: {}", e.getMessage());
                }
            }
            List<com.huobao.drama.entity.Character> existing = characterMapper.selectList(
                    new LambdaQueryWrapper<com.huobao.drama.entity.Character>()
                            .eq(com.huobao.drama.entity.Character::getDramaId, dramaId));
            Map<String, com.huobao.drama.entity.Character> existingMap = new HashMap<>();
            for (com.huobao.drama.entity.Character c : existing) {
                existingMap.put(c.getName(), c);
            }

            int created = 0, skipped = 0, rejected = 0;
            StringBuilder rejectedNames = new StringBuilder();
            for (Map<String, Object> charData : chars) {
                String name = (String) charData.get("name");
                String role = (String) charData.get("role");
                String charPrompt = prompts.getOrDefault(name, (String) charData.get("prompt"));

                // 验证：只接受主角/配角，且必须有prompt
                if (role != null && role.contains("龙套")) {
                    rejected++;
                    rejectedNames.append(name).append("(龙套), ");
                    continue;
                }
                if (charPrompt == null || charPrompt.trim().isEmpty()) {
                    rejected++;
                    rejectedNames.append(name).append("(无prompt), ");
                    continue;
                }

                if (existingMap.containsKey(name)) {
                    com.huobao.drama.entity.Character ec = existingMap.get(name);
                    linkCharacterToEpisode(ec.getId(), episodeId);
                    skipped++;
                } else {
                    com.huobao.drama.entity.Character c = new com.huobao.drama.entity.Character();
                    c.setDramaId(dramaId);
                    c.setName(name);
                    c.setRole(role);
                    c.setDescription((String) charData.get("description"));
                    c.setAppearance((String) charData.get("appearance"));
                    c.setPrompt(charPrompt);
                    c.setPersonality((String) charData.get("personality"));
                    characterMapper.insert(c);
                    linkCharacterToEpisode(c.getId(), episodeId);
                    created++;
                }
            }
            String result = "Characters saved: " + created + " created, " + skipped + " existing linked";
            if (rejected > 0) {
                result += ", " + rejected + " rejected (" + rejectedNames.toString() + ")";
            }
            return result;
        } catch (Exception e) {
            log.error("Error saving characters", e);
            if (e.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException) {
                return "Error parsing JSON: " + e.getCause().getMessage() + ". Please check JSON format.";
            }
            return "Error saving characters: " + e.getMessage();
        }
    }

    @Tool(description = """
            保存场景并关联到当前集，相同location会更新prompt。
            scenesJson必须是严格的JSON数组格式，字段名必须用双引号包裹，例如：
            [{"location":"客厅","time":"白天","prompt":"..."}]
            """)
    public String saveDedupScenes(Long dramaId, Long episodeId, String scenesJson) {
        try {
            if (scenesJson == null || scenesJson.trim().isEmpty()) {
                return "Error: scenesJson is empty";
            }
            log.info("Parsing scenesJson: {}", scenesJson.length() > 200 ? scenesJson.substring(0, 200) : scenesJson);
            List<Map<String, Object>> sceneList = objectMapper.readValue(scenesJson, List.class);
            List<Scene> existing = sceneMapper.selectList(
                    new LambdaQueryWrapper<Scene>().eq(Scene::getDramaId, dramaId));
            Map<String, Scene> existingMap = new HashMap<>();
            for (Scene s : existing) {
                existingMap.put(s.getLocation(), s);
            }

            int created = 0, updated = 0;
            for (Map<String, Object> sceneData : sceneList) {
                String location = (String) sceneData.get("location");
                String time = (String) sceneData.get("time");
                String prompt = (String) sceneData.get("prompt");

                if (existingMap.containsKey(location)) {
                    Scene es = existingMap.get(location);
                    es.setTime(time);
                    es.setPrompt(prompt);
                    sceneMapper.updateById(es);
                    linkSceneToEpisode(es.getId(), episodeId);
                    updated++;
                } else {
                    Scene s = new Scene();
                    s.setDramaId(dramaId);
                    s.setLocation(location);
                    s.setTime(time);
                    s.setPrompt(prompt);
                    sceneMapper.insert(s);
                    linkSceneToEpisode(s.getId(), episodeId);
                    created++;
                }
            }
            return "Scenes saved: " + created + " created, " + updated + " updated";
        } catch (Exception e) {
            return "Error saving scenes: " + e.getMessage();
        }
    }

    @Tool(description = """
            保存道具，按名称自动去重。
            propsJson必须是严格的JSON数组格式，字段名必须用双引号包裹，例如：
            [{"name":"宝剑","type":"武器","description":"...","prompt":"..."}]
            """)
    public String saveDedupProps(Long dramaId, String propsJson) {
        try {
            if (propsJson == null || propsJson.trim().isEmpty()) {
                return "Error: propsJson is empty";
            }
            log.info("Parsing propsJson: {}", propsJson.length() > 200 ? propsJson.substring(0, 200) : propsJson);
            List<Map<String, Object>> propList = objectMapper.readValue(propsJson, List.class);
            List<Prop> existing = propMapper.selectList(
                    new LambdaQueryWrapper<Prop>().eq(Prop::getDramaId, dramaId));
            Set<String> existingNames = new HashSet<>();
            for (Prop p : existing) {
                existingNames.add(p.getName());
            }

            int created = 0, skipped = 0;
            for (Map<String, Object> propData : propList) {
                String name = (String) propData.get("name");
                if (existingNames.contains(name)) {
                    skipped++;
                } else {
                    Prop p = new Prop();
                    p.setDramaId(dramaId);
                    p.setName(name);
                    p.setType((String) propData.get("type"));
                    p.setDescription((String) propData.get("description"));
                    p.setPrompt((String) propData.get("prompt"));
                    propMapper.insert(p);
                    created++;
                }
            }
            return "Props saved: " + created + " created, " + skipped + " existing skipped";
        } catch (Exception e) {
            return "Error saving props: " + e.getMessage();
        }
    }

    private void linkCharacterToEpisode(Long characterId, Long episodeId) {
        Long count = episodeCharacterMapper.selectCount(
                new LambdaQueryWrapper<EpisodeCharacter>()
                        .eq(EpisodeCharacter::getEpisodeId, episodeId)
                        .eq(EpisodeCharacter::getCharacterId, characterId));
        if (count == 0) {
            EpisodeCharacter ec = new EpisodeCharacter();
            ec.setEpisodeId(episodeId);
            ec.setCharacterId(characterId);
            episodeCharacterMapper.insert(ec);
        }
    }

    private void linkSceneToEpisode(Long sceneId, Long episodeId) {
        Long count = episodeSceneMapper.selectCount(
                new LambdaQueryWrapper<EpisodeScene>()
                        .eq(EpisodeScene::getEpisodeId, episodeId)
                        .eq(EpisodeScene::getSceneId, sceneId));
        if (count == 0) {
            EpisodeScene es = new EpisodeScene();
            es.setEpisodeId(episodeId);
            es.setSceneId(sceneId);
            episodeSceneMapper.insert(es);
        }
    }
}


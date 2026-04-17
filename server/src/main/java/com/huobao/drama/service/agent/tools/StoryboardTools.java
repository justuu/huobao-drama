package com.huobao.drama.service.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.entity.*;
import com.huobao.drama.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class StoryboardTools {

    private final EpisodeMapper episodeMapper;
    private final CharacterMapper characterMapper;
    private final SceneMapper sceneMapper;
    private final PropMapper propMapper;
    private final StoryboardMapper storyboardMapper;
    private final StoryboardCharacterMapper storyboardCharacterMapper;
    private final SeedanceAssetMapper seedanceAssetMapper;
    private final ObjectMapper objectMapper;

    @Tool(description = "获取剧集的所有分镜信息")
    public String getStoryboards(Long episodeId, Long dramaId) {
        return readStoryboardContext(episodeId, dramaId);
    }

    @Tool(description = "为剧集创建分镜")
    public String createStoryboard(Long episodeId, Long dramaId, String storyboardsJson) {
        return saveStoryboards(episodeId, dramaId, storyboardsJson);
    }

    @Tool(description = "更新分镜信息")
    public String updateStoryboardTool(Long storyboardId, String updatesJson) {
        try {
            Storyboard sb = storyboardMapper.selectById(storyboardId);
            if (sb == null) return "Storyboard not found";
            Map<String, Object> updates = objectMapper.readValue(updatesJson, Map.class);
            if (updates.containsKey("prompt_text")) sb.setPromptText((String) updates.get("prompt_text"));
            if (updates.containsKey("dialogue")) sb.setDialogue((String) updates.get("dialogue"));
            if (updates.containsKey("action")) sb.setAction((String) updates.get("action"));
            if (updates.containsKey("duration_ms") && updates.get("duration_ms") instanceof Number) sb.setDurationMs(((Number) updates.get("duration_ms")).intValue());
            if (updates.containsKey("group_id")) sb.setGroupId((String) updates.get("group_id"));
            if (updates.containsKey("group_index") && updates.get("group_index") instanceof Number) sb.setGroupIndex(((Number) updates.get("group_index")).intValue());
            sb.setUpdatedAt(LocalDateTime.now());
            storyboardMapper.updateById(sb);
            return "Storyboard updated";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public String readStoryboardContext(Long episodeId, Long dramaId) {
        Episode ep = episodeMapper.selectById(episodeId);
        String script = ep != null ? ep.getScriptContent() : "";

        List<com.huobao.drama.entity.Character> chars = characterMapper.selectList(
                new LambdaQueryWrapper<com.huobao.drama.entity.Character>()
                        .eq(com.huobao.drama.entity.Character::getDramaId, dramaId));
        List<Scene> scenes = sceneMapper.selectList(
                new LambdaQueryWrapper<Scene>().eq(Scene::getDramaId, dramaId));
        List<Prop> props = propMapper.selectList(
                new LambdaQueryWrapper<Prop>().eq(Prop::getDramaId, dramaId));

        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("script", script);
            context.put("characters", chars);
            context.put("scenes", scenes);
            context.put("props", props);
            return objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            return "Error building context: " + e.getMessage();
        }
    }

    public String readAssetMapping(Long dramaId) {
        List<SeedanceAsset> assets = seedanceAssetMapper.selectList(
                new LambdaQueryWrapper<SeedanceAsset>()
                        .eq(SeedanceAsset::getDramaId, dramaId)
                        .eq(SeedanceAsset::getStatus, "ready"));
        Map<String, String> mapping = new LinkedHashMap<>();
        for (SeedanceAsset a : assets) {
            mapping.put(a.getName(), a.getAssetId());
        }
        try {
            return objectMapper.writeValueAsString(mapping);
        } catch (Exception e) {
            return "{}";
        }
    }

    public String readFormattedScript(Long episodeId) {
        Episode ep = episodeMapper.selectById(episodeId);
        if (ep == null) return "Episode not found";
        String script = ep.getScriptContent();
        return script != null ? script : "No formatted script available";
    }

    public String readCharacters(Long dramaId) {
        List<com.huobao.drama.entity.Character> chars = characterMapper.selectList(
                new LambdaQueryWrapper<com.huobao.drama.entity.Character>()
                        .eq(com.huobao.drama.entity.Character::getDramaId, dramaId));
        try {
            return objectMapper.writeValueAsString(chars);
        } catch (Exception e) {
            return "[]";
        }
    }

    public String readScenes(Long dramaId) {
        List<Scene> scenes = sceneMapper.selectList(
                new LambdaQueryWrapper<Scene>().eq(Scene::getDramaId, dramaId));
        try {
            return objectMapper.writeValueAsString(scenes);
        } catch (Exception e) {
            return "[]";
        }
    }

    public String saveStoryboards(Long episodeId, Long dramaId, String storyboardsJson) {
        try {
            List<Map<String, Object>> boards = objectMapper.readValue(storyboardsJson, List.class);
            int count = 0;
            for (Map<String, Object> data : boards) {
                Storyboard sb = new Storyboard();
                sb.setEpisodeId(episodeId);
                sb.setStoryboardNumber(count + 1);
                sb.setTitle((String) data.get("title"));
                sb.setShotType((String) data.get("shot_type"));
                sb.setAngle((String) data.get("angle"));
                sb.setMovement((String) data.get("movement"));
                sb.setLocation((String) data.get("location"));
                sb.setTime((String) data.get("time"));
                sb.setAction((String) data.get("action"));
                sb.setDialogue((String) data.get("dialogue"));
                sb.setDescription((String) data.get("description"));
                sb.setResult((String) data.get("result"));
                sb.setAtmosphere((String) data.get("atmosphere"));
                sb.setVideoPrompt((String) data.get("video_prompt"));
                sb.setBgmPrompt((String) data.get("bgm_prompt"));
                sb.setSoundEffect((String) data.get("sound_effect"));
                sb.setPromptText((String) data.get("prompt_text"));
                sb.setGroupId((String) data.get("group_id"));
                if (data.get("duration") instanceof Number) sb.setDuration(((Number) data.get("duration")).intValue());
                if (data.get("duration_ms") instanceof Number) sb.setDurationMs(((Number) data.get("duration_ms")).intValue());
                if (data.get("group_index") instanceof Number) sb.setGroupIndex(((Number) data.get("group_index")).intValue());
                if (data.get("scene_id") instanceof Number) sb.setSceneId(((Number) data.get("scene_id")).longValue());
                storyboardMapper.insert(sb);

                Object charIds = data.get("character_ids");
                if (charIds instanceof List) {
                    for (Object id : (List<?>) charIds) {
                        StoryboardCharacter sc = new StoryboardCharacter();
                        sc.setStoryboardId(sb.getId());
                        sc.setCharacterId(((Number) id).longValue());
                        storyboardCharacterMapper.insert(sc);
                    }
                }
                count++;
            }
            return "Saved " + count + " storyboards";
        } catch (Exception e) {
            return "Error saving storyboards: " + e.getMessage();
        }
    }
}

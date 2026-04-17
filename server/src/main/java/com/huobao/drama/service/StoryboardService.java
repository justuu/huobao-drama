package com.huobao.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.entity.Storyboard;
import com.huobao.drama.entity.StoryboardCharacter;
import com.huobao.drama.mapper.StoryboardMapper;
import com.huobao.drama.mapper.StoryboardCharacterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StoryboardService {

    private final StoryboardMapper storyboardMapper;
    private final StoryboardCharacterMapper storyboardCharacterMapper;

    public Storyboard createStoryboard(Map<String, Object> body) {
        Storyboard sb = new Storyboard();
        if (body.get("episode_id") != null) sb.setEpisodeId(Long.valueOf(body.get("episode_id").toString()));
        if (body.get("storyboard_number") != null) sb.setStoryboardNumber(Integer.valueOf(body.get("storyboard_number").toString()));
        if (body.get("title") != null) sb.setTitle(body.get("title").toString());
        if (body.get("description") != null) sb.setDescription(body.get("description").toString());
        if (body.get("action") != null) sb.setAction(body.get("action").toString());
        if (body.get("dialogue") != null) sb.setDialogue(body.get("dialogue").toString());
        if (body.get("scene_id") != null) sb.setSceneId(Long.valueOf(body.get("scene_id").toString()));
        if (body.get("duration") != null) sb.setDuration(Integer.valueOf(body.get("duration").toString()));
        storyboardMapper.insert(sb);

        Object charIdsObj = body.get("character_ids");
        if (charIdsObj instanceof List<?> charIds) {
            for (Object cid : charIds) {
                StoryboardCharacter sc = new StoryboardCharacter();
                sc.setStoryboardId(sb.getId());
                sc.setCharacterId(Long.valueOf(cid.toString()));
                storyboardCharacterMapper.insert(sc);
            }
        }
        return sb;
    }

    public boolean updateStoryboard(Long id, Map<String, Object> body) {
        Storyboard sb = storyboardMapper.selectById(id);
        if (sb == null) return false;

        if (body.containsKey("title")) sb.setTitle((String) body.get("title"));
        if (body.containsKey("description")) sb.setDescription((String) body.get("description"));
        if (body.containsKey("shot_type")) sb.setShotType((String) body.get("shot_type"));
        if (body.containsKey("angle")) sb.setAngle((String) body.get("angle"));
        if (body.containsKey("movement")) sb.setMovement((String) body.get("movement"));
        if (body.containsKey("action")) sb.setAction((String) body.get("action"));
        if (body.containsKey("dialogue")) sb.setDialogue((String) body.get("dialogue"));
        if (body.containsKey("duration") && body.get("duration") != null)
            sb.setDuration(Integer.valueOf(body.get("duration").toString()));
        if (body.containsKey("video_prompt")) sb.setVideoPrompt((String) body.get("video_prompt"));
        if (body.containsKey("scene_id") && body.get("scene_id") != null)
            sb.setSceneId(Long.valueOf(body.get("scene_id").toString()));
        if (body.containsKey("location")) sb.setLocation((String) body.get("location"));
        if (body.containsKey("time")) sb.setTime((String) body.get("time"));
        if (body.containsKey("atmosphere")) sb.setAtmosphere((String) body.get("atmosphere"));
        if (body.containsKey("result")) sb.setResult((String) body.get("result"));
        if (body.containsKey("bgm_prompt")) sb.setBgmPrompt((String) body.get("bgm_prompt"));
        if (body.containsKey("sound_effect")) sb.setSoundEffect((String) body.get("sound_effect"));
        if (body.containsKey("prompt_text")) sb.setPromptText((String) body.get("prompt_text"));
        if (body.containsKey("duration_ms") && body.get("duration_ms") != null)
            sb.setDurationMs(Integer.valueOf(body.get("duration_ms").toString()));
        if (body.containsKey("group_id")) sb.setGroupId((String) body.get("group_id"));
        if (body.containsKey("group_index") && body.get("group_index") != null)
            sb.setGroupIndex(Integer.valueOf(body.get("group_index").toString()));

        storyboardMapper.updateById(sb);
        return true;
    }

    public boolean deleteStoryboard(Long id) {
        Storyboard sb = storyboardMapper.selectById(id);
        if (sb == null) return false;
        storyboardCharacterMapper.delete(
                new LambdaQueryWrapper<StoryboardCharacter>().eq(StoryboardCharacter::getStoryboardId, id));
        storyboardMapper.deleteById(id);
        return true;
    }
}

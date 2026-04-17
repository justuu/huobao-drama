package com.huobao.drama.service;

import com.huobao.drama.entity.Scene;
import com.huobao.drama.mapper.SceneMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SceneService {

    private final SceneMapper sceneMapper;

    public Scene create(Map<String, Object> body) {
        Scene scene = new Scene();
        Object dramaId = body.get("drama_id");
        if (dramaId instanceof Number) scene.setDramaId(((Number) dramaId).longValue());
        Object episodeId = body.get("episode_id");
        if (episodeId instanceof Number) scene.setEpisodeId(((Number) episodeId).longValue());
        scene.setLocation((String) body.get("location"));
        scene.setTime((String) body.get("time"));
        scene.setPrompt((String) body.get("prompt"));
        sceneMapper.insert(scene);
        return scene;
    }

    public boolean update(Long id, Map<String, Object> body) {
        Scene scene = sceneMapper.selectById(id);
        if (scene == null) return false;

        if (body.containsKey("location")) scene.setLocation((String) body.get("location"));
        if (body.containsKey("time")) scene.setTime((String) body.get("time"));
        if (body.containsKey("prompt")) scene.setPrompt((String) body.get("prompt"));

        sceneMapper.updateById(scene);
        return true;
    }

    public boolean delete(Long id) {
        Scene scene = sceneMapper.selectById(id);
        if (scene == null) return false;
        sceneMapper.deleteById(id);
        return true;
    }

    public Scene regeneratePrompt(Long id) {
        Scene scene = sceneMapper.selectById(id);
        if (scene == null) {
            return null;
        }

        // TODO: 调用 agent 重新生成 prompt
        // 暂时返回原对象，后续实现

        return scene;
    }
}

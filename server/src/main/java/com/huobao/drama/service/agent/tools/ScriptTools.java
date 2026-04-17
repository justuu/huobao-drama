package com.huobao.drama.service.agent.tools;

import com.huobao.drama.entity.Episode;
import com.huobao.drama.mapper.EpisodeMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ScriptTools {

    private final EpisodeMapper episodeMapper;

    @Tool(description = "获取剧集的剧本内容")
    public String getScript(Long episodeId) {
        Episode ep = episodeMapper.selectById(episodeId);
        if (ep == null) return "Episode not found";
        String script = ep.getScriptContent();
        return script != null ? script : "No script available";
    }

    @Tool(description = "更新剧集的剧本内容")
    public String updateScript(Long episodeId, String content) {
        Episode ep = episodeMapper.selectById(episodeId);
        if (ep == null) return "Episode not found";
        ep.setScriptContent(content);
        ep.setUpdatedAt(LocalDateTime.now());
        episodeMapper.updateById(ep);
        return "Script updated successfully";
    }
}

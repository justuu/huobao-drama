package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.Episode;
import com.huobao.drama.entity.Scene;
import com.huobao.drama.service.EpisodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/episodes")
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;

    @PostMapping
    public ApiResponse<Episode> createEpisode(@RequestBody Map<String, Object> body) {
        Long dramaId = body.get("drama_id") == null ? null : Long.valueOf(body.get("drama_id").toString());
        if (dramaId == null) {
            return ApiResponse.badRequest("drama_id is required");
        }
        Long imageConfigId = body.get("image_config_id") == null ? null : Long.valueOf(body.get("image_config_id").toString());
        Long videoConfigId = body.get("video_config_id") == null ? null : Long.valueOf(body.get("video_config_id").toString());
        Long audioConfigId = body.get("audio_config_id") == null ? null : Long.valueOf(body.get("audio_config_id").toString());
        String title = body.get("title") == null ? null : body.get("title").toString();

        Episode episode = episodeService.createEpisode(dramaId, imageConfigId, videoConfigId, audioConfigId, title);
        return ApiResponse.created(episode);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateEpisode(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean found = episodeService.updateEpisode(id, body);
        if (!found) {
            return ApiResponse.notFound("Episode not found");
        }
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/characters")
    public ApiResponse<List<com.huobao.drama.entity.Character>> getCharacters(@PathVariable Long id) {
        return ApiResponse.ok(episodeService.getEpisodeCharacters(id));
    }

    @GetMapping("/{id}/scenes")
    public ApiResponse<List<Scene>> getScenes(@PathVariable Long id) {
        return ApiResponse.ok(episodeService.getEpisodeScenes(id));
    }

    @GetMapping("/{episodeId}/storyboards")
    public ApiResponse<List<Map<String, Object>>> getStoryboards(@PathVariable Long episodeId) {
        return ApiResponse.ok(episodeService.getEpisodeStoryboards(episodeId));
    }

    @GetMapping("/{id}/pipeline-status")
    public ApiResponse<Map<String, Object>> getPipelineStatus(@PathVariable Long id) {
        Map<String, Object> status = episodeService.getPipelineStatus(id);
        if (status == null) {
            return ApiResponse.notFound("Episode not found");
        }
        return ApiResponse.ok(status);
    }
}

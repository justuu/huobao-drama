package com.huobao.drama.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.VideoGeneration;
import com.huobao.drama.mapper.VideoGenerationMapper;
import com.huobao.drama.service.seedance.SeedanceVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoGenerationMapper videoGenerationMapper;
    private final SeedanceVideoService seedanceVideoService;

    @PostMapping
    public ApiResponse<VideoGeneration> createVideo(@RequestBody Map<String, Object> body) {
        VideoGeneration vid = new VideoGeneration();
        if (body.get("prompt") != null) vid.setPrompt(body.get("prompt").toString());
        if (body.get("storyboard_id") != null) vid.setStoryboardId(Long.valueOf(body.get("storyboard_id").toString()));
        if (body.get("drama_id") != null) vid.setDramaId(Long.valueOf(body.get("drama_id").toString()));
        if (body.get("model") != null) vid.setModel(body.get("model").toString());
        if (body.get("duration") != null) vid.setDuration(Integer.valueOf(body.get("duration").toString()));
        if (body.get("aspect_ratio") != null) vid.setAspectRatio(body.get("aspect_ratio").toString());
        vid.setStatus("pending");
        videoGenerationMapper.insert(vid);
        return ApiResponse.created(vid);
    }

    @GetMapping("/{id}")
    public ApiResponse<VideoGeneration> getVideo(@PathVariable Long id) {
        VideoGeneration vid = videoGenerationMapper.selectById(id);
        if (vid == null) return ApiResponse.notFound("Video generation not found");
        return ApiResponse.ok(vid);
    }

    @GetMapping
    public ApiResponse<List<VideoGeneration>> listVideos(
            @RequestParam(required = false) Long storyboard_id,
            @RequestParam(required = false) Long drama_id) {
        QueryWrapper<VideoGeneration> qw = new QueryWrapper<>();
        if (storyboard_id != null) qw.eq("storyboard_id", storyboard_id);
        if (drama_id != null) qw.eq("drama_id", drama_id);
        return ApiResponse.ok(videoGenerationMapper.selectList(qw));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteVideo(@PathVariable Long id) {
        videoGenerationMapper.deleteById(id);
        return ApiResponse.ok();
    }

    @PostMapping("/seedance")
    public ApiResponse<VideoGeneration> generateSeedanceVideo(@RequestBody Map<String, Object> body) {
        String groupId = (String) body.get("group_id");
        Long dramaId = ((Number) body.get("drama_id")).longValue();
        VideoGeneration vg = seedanceVideoService.generateForGroup(groupId, dramaId);
        return ApiResponse.ok(vg);
    }

    @GetMapping("/seedance/{id}/status")
    public ApiResponse<VideoGeneration> pollSeedanceStatus(@PathVariable Long id) {
        VideoGeneration vg = seedanceVideoService.pollStatus(id);
        if (vg == null) return ApiResponse.notFound("Video generation not found");
        return ApiResponse.ok(vg);
    }
}

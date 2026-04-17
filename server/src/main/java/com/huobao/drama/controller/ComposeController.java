package com.huobao.drama.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.Storyboard;
import com.huobao.drama.mapper.StoryboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/compose")
@RequiredArgsConstructor
public class ComposeController {

    private final StoryboardMapper storyboardMapper;

    @PostMapping("/storyboards/{id}/compose")
    public ApiResponse<Map<String, Object>> composeStoryboard(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("message", "not implemented yet");
        return ApiResponse.ok(result);
    }

    @PostMapping("/episodes/{id}/compose-all")
    public ApiResponse<Map<String, Object>> composeAllEpisode(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "not implemented yet");
        return ApiResponse.ok(result);
    }

    @GetMapping("/episodes/{id}/compose-status")
    public ApiResponse<Map<String, Object>> getComposeStatus(@PathVariable Long id) {
        QueryWrapper<Storyboard> qw = new QueryWrapper<>();
        qw.eq("episode_id", id);
        List<Storyboard> storyboards = storyboardMapper.selectList(qw);

        long total = storyboards.size();
        long completed = storyboards.stream().filter(s -> "completed".equals(s.getStatus())).count();
        long failed = storyboards.stream().filter(s -> "failed".equals(s.getStatus())).count();
        long processing = storyboards.stream().filter(s -> "processing".equals(s.getStatus())).count();
        long idle = storyboards.stream().filter(s -> s.getStatus() == null || "idle".equals(s.getStatus()) || "pending".equals(s.getStatus())).count();

        List<Map<String, Object>> items = storyboards.stream().map(s -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("title", s.getTitle());
            item.put("status", s.getStatus());
            item.put("storyboard_number", s.getStoryboardNumber());
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("completed", completed);
        result.put("failed", failed);
        result.put("processing", processing);
        result.put("idle", idle);
        result.put("items", items);
        return ApiResponse.ok(result);
    }
}

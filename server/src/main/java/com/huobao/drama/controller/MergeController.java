package com.huobao.drama.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.VideoMerge;
import com.huobao.drama.mapper.VideoMergeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/merge")
@RequiredArgsConstructor
public class MergeController {

    private final VideoMergeMapper videoMergeMapper;

    @PostMapping("/episodes/{id}/merge")
    public ApiResponse<Map<String, Object>> mergeEpisode(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "not implemented yet");
        return ApiResponse.ok(result);
    }

    @GetMapping("/episodes/{id}/merge")
    public ApiResponse<VideoMerge> getMergeStatus(@PathVariable Long id) {
        QueryWrapper<VideoMerge> qw = new QueryWrapper<>();
        qw.eq("episode_id", id).orderByDesc("created_at").last("LIMIT 1");
        List<VideoMerge> list = videoMergeMapper.selectList(qw);
        if (list.isEmpty()) return ApiResponse.notFound("No merge record found for episode");
        return ApiResponse.ok(list.get(0));
    }
}

package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.Storyboard;
import com.huobao.drama.service.StoryboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/storyboards")
@RequiredArgsConstructor
public class StoryboardController {

    private final StoryboardService storyboardService;

    @PostMapping
    public ApiResponse<Storyboard> createStoryboard(@RequestBody Map<String, Object> body) {
        Storyboard sb = storyboardService.createStoryboard(body);
        return ApiResponse.created(sb);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateStoryboard(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean found = storyboardService.updateStoryboard(id, body);
        if (!found) return ApiResponse.notFound("Storyboard not found");
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/generate-tts")
    public ApiResponse<String> generateTts(@PathVariable Long id) {
        return ApiResponse.ok("not implemented yet");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteStoryboard(@PathVariable Long id) {
        boolean found = storyboardService.deleteStoryboard(id);
        if (!found) return ApiResponse.notFound("Storyboard not found");
        return ApiResponse.ok();
    }
}

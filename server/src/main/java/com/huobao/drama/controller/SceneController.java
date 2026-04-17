package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.ImageGeneration;
import com.huobao.drama.entity.Scene;
import com.huobao.drama.service.ImageGenerationService;
import com.huobao.drama.service.SceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/scenes")
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;
    private final ImageGenerationService imageGenerationService;

    @PostMapping
    public ApiResponse<Scene> createScene(@RequestBody Map<String, Object> body) {
        Scene scene = sceneService.create(body);
        return ApiResponse.ok(scene);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateScene(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean found = sceneService.update(id, body);
        if (!found) return ApiResponse.notFound("Scene not found");
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/generate-image")
    public ApiResponse<ImageGeneration> generateImage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        ImageGeneration img = imageGenerationService.generateForScene(id, body);
        return ApiResponse.ok(img);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteScene(@PathVariable Long id) {
        boolean found = sceneService.delete(id);
        if (!found) return ApiResponse.notFound("Scene not found");
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/regenerate-prompt")
    public ApiResponse<Scene> regeneratePrompt(@PathVariable Long id) {
        Scene scene = sceneService.regeneratePrompt(id);
        if (scene == null) {
            return ApiResponse.notFound("Scene not found");
        }
        return ApiResponse.ok(scene);
    }
}

package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.ImageGeneration;
import com.huobao.drama.entity.Prop;
import com.huobao.drama.service.ImageGenerationService;
import com.huobao.drama.service.PropService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/props")
@RequiredArgsConstructor
public class PropController {

    private final PropService propService;
    private final ImageGenerationService imageGenerationService;

    @GetMapping
    public ApiResponse<List<Prop>> listProps(@RequestParam("drama_id") Long dramaId) {
        return ApiResponse.ok(propService.listByDrama(dramaId));
    }

    @PostMapping
    public ApiResponse<Prop> createProp(@RequestBody Map<String, Object> body) {
        Prop prop = propService.create(body);
        return ApiResponse.ok(prop);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateProp(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean found = propService.update(id, body);
        if (!found) return ApiResponse.notFound("Prop not found");
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProp(@PathVariable Long id) {
        boolean found = propService.delete(id);
        if (!found) return ApiResponse.notFound("Prop not found");
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/generate-image")
    public ApiResponse<ImageGeneration> generateImage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        ImageGeneration img = imageGenerationService.generateForProp(id, body);
        return ApiResponse.ok(img);
    }

    @PostMapping("/{id}/regenerate-prompt")
    public ApiResponse<Prop> regeneratePrompt(@PathVariable Long id) {
        Prop prop = propService.regeneratePrompt(id);
        if (prop == null) {
            return ApiResponse.notFound("Prop not found");
        }
        return ApiResponse.ok(prop);
    }
}

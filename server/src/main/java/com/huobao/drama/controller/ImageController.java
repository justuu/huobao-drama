package com.huobao.drama.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.ImageGeneration;
import com.huobao.drama.mapper.ImageGenerationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageGenerationMapper imageGenerationMapper;

    @PostMapping
    public ApiResponse<ImageGeneration> createImage(@RequestBody Map<String, Object> body) {
        ImageGeneration img = new ImageGeneration();
        if (body.get("prompt") != null) img.setPrompt(body.get("prompt").toString());
        if (body.get("storyboard_id") != null) img.setStoryboardId(Long.valueOf(body.get("storyboard_id").toString()));
        if (body.get("scene_id") != null) img.setSceneId(Long.valueOf(body.get("scene_id").toString()));
        if (body.get("character_id") != null) img.setCharacterId(Long.valueOf(body.get("character_id").toString()));
        if (body.get("drama_id") != null) img.setDramaId(Long.valueOf(body.get("drama_id").toString()));
        if (body.get("frame_type") != null) img.setFrameType(body.get("frame_type").toString());
        if (body.get("model") != null) img.setModel(body.get("model").toString());
        if (body.get("size") != null) img.setSize(body.get("size").toString());
        if (body.get("reference_images") != null) img.setReferenceImages(body.get("reference_images").toString());
        img.setStatus("pending");
        imageGenerationMapper.insert(img);
        return ApiResponse.created(img);
    }

    @GetMapping("/{id}")
    public ApiResponse<ImageGeneration> getImage(@PathVariable Long id) {
        ImageGeneration img = imageGenerationMapper.selectById(id);
        if (img == null) return ApiResponse.notFound("Image generation not found");
        return ApiResponse.ok(img);
    }

    @GetMapping
    public ApiResponse<List<ImageGeneration>> listImages(
            @RequestParam(required = false) Long storyboard_id,
            @RequestParam(required = false) Long drama_id) {
        QueryWrapper<ImageGeneration> qw = new QueryWrapper<>();
        if (storyboard_id != null) qw.eq("storyboard_id", storyboard_id);
        if (drama_id != null) qw.eq("drama_id", drama_id);
        return ApiResponse.ok(imageGenerationMapper.selectList(qw));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteImage(@PathVariable Long id) {
        imageGenerationMapper.deleteById(id);
        return ApiResponse.ok();
    }
}

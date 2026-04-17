package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.Character;
import com.huobao.drama.entity.ImageGeneration;
import com.huobao.drama.service.CharacterService;
import com.huobao.drama.service.ImageGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;
    private final ImageGenerationService imageGenerationService;

    @PutMapping("/{id}")
    public ApiResponse<Void> updateCharacter(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean found = characterService.update(id, body);
        if (!found) return ApiResponse.notFound("Character not found");
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCharacter(@PathVariable Long id) {
        boolean found = characterService.delete(id);
        if (!found) return ApiResponse.notFound("Character not found");
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/generate-voice-sample")
    public ApiResponse<Map<String, String>> generateVoiceSample(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(Map.of("message", "not implemented yet"));
    }

    @PostMapping("/{id}/generate-image")
    public ApiResponse<ImageGeneration> generateImage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        ImageGeneration img = imageGenerationService.generateForCharacter(id, body);
        return ApiResponse.ok(img);
    }

    @PostMapping("/batch-generate-images")
    public ApiResponse<Map<String, String>> batchGenerateImages(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(Map.of("message", "not implemented yet"));
    }

    @PostMapping("/{id}/regenerate-prompt")
    public ApiResponse<Character> regeneratePrompt(@PathVariable Long id) {
        Character character = characterService.regeneratePrompt(id);
        if (character == null) {
            return ApiResponse.notFound("Character not found");
        }
        return ApiResponse.ok(character);
    }
}

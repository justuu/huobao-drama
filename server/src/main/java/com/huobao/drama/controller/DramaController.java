package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.Drama;
import com.huobao.drama.service.DramaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dramas")
@RequiredArgsConstructor
public class DramaController {

    private final DramaService dramaService;

    // GET /api/v1/dramas
    @GetMapping
    public ApiResponse<Map<String, Object>> listDramas(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(dramaService.listDramas(page, pageSize, status, keyword));
    }

    // POST /api/v1/dramas
    @PostMapping
    public ApiResponse<Drama> createDrama(@RequestBody Map<String, Object> body) {
        Drama drama = dramaService.createDrama(body);
        return ApiResponse.created(drama);
    }

    // GET /api/v1/dramas/stats
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.ok(dramaService.getStats());
    }

    // GET /api/v1/dramas/:id
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getDrama(@PathVariable Long id) {
        Map<String, Object> detail = dramaService.getDramaDetail(id);
        if (detail == null) {
            return ApiResponse.notFound("Drama not found");
        }
        return ApiResponse.ok(detail);
    }

    // PUT /api/v1/dramas/:id
    @PutMapping("/{id}")
    public ApiResponse<Void> updateDrama(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean found = dramaService.updateDrama(id, body);
        if (!found) {
            return ApiResponse.notFound("Drama not found");
        }
        return ApiResponse.ok();
    }

    // DELETE /api/v1/dramas/:id
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDrama(@PathVariable Long id) {
        boolean found = dramaService.deleteDrama(id);
        if (!found) {
            return ApiResponse.notFound("Drama not found");
        }
        return ApiResponse.ok();
    }

    // PUT /api/v1/dramas/:id/characters
    @PutMapping("/{id}/characters")
    public ApiResponse<Void> batchSaveCharacters(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> characters = (List<Map<String, Object>>) body.get("characters");
        if (characters == null) {
            return ApiResponse.badRequest("characters field is required");
        }
        dramaService.batchSaveCharacters(id, characters);
        return ApiResponse.ok();
    }

    // PUT /api/v1/dramas/:id/episodes
    @PutMapping("/{id}/episodes")
    public ApiResponse<Void> batchSaveEpisodes(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> episodes = (List<Map<String, Object>>) body.get("episodes");
        if (episodes == null) {
            return ApiResponse.badRequest("episodes field is required");
        }
        dramaService.batchSaveEpisodes(id, episodes);
        return ApiResponse.ok();
    }
}

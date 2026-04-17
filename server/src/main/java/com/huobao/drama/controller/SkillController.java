package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public ApiResponse<List<Map<String, String>>> listSkills() {
        try {
            return ApiResponse.ok(skillService.listSkills());
        } catch (IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, String>> getSkill(@PathVariable String id) {
        try {
            Map<String, String> skill = skillService.getSkill(id);
            if (skill == null) return ApiResponse.notFound("Skill not found");
            return ApiResponse.ok(skill);
        } catch (IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateSkill(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            skillService.updateSkill(id, body.get("content"));
            return ApiResponse.ok();
        } catch (IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<Map<String, String>> createSkill(@RequestBody Map<String, String> body) {
        try {
            Map<String, String> skill = skillService.createSkill(
                    body.get("id"), body.get("name"), body.get("description"));
            return ApiResponse.created(skill);
        } catch (IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSkill(@PathVariable String id) {
        try {
            skillService.deleteSkill(id);
            return ApiResponse.ok();
        } catch (IOException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}

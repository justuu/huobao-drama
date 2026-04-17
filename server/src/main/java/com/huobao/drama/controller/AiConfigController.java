package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.AiServiceConfig;
import com.huobao.drama.service.AiConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai-configs")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigService aiConfigService;

    @GetMapping
    public ApiResponse<List<AiServiceConfig>> listConfigs(
            @RequestParam(value = "service_type", required = false) String serviceType) {
        return ApiResponse.ok(aiConfigService.listConfigs(serviceType));
    }

    @PostMapping
    public ApiResponse<AiServiceConfig> createConfig(@RequestBody Map<String, Object> body) {
        return ApiResponse.created(aiConfigService.createConfig(body));
    }

    @PostMapping("/test")
    public ApiResponse<String> testConfig() {
        return ApiResponse.ok("not implemented yet");
    }

    @PostMapping("/huobao-preset")
    public ApiResponse<String> huobaoPreset() {
        return ApiResponse.ok("not implemented yet");
    }

    @GetMapping("/{id}")
    public ApiResponse<AiServiceConfig> getConfig(@PathVariable Long id) {
        AiServiceConfig config = aiConfigService.getConfig(id);
        if (config == null) return ApiResponse.notFound("Config not found");
        return ApiResponse.ok(config);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateConfig(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean found = aiConfigService.updateConfig(id, body);
        if (!found) return ApiResponse.notFound("Config not found");
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        boolean deleted = aiConfigService.deleteConfig(id);
        if (!deleted) return ApiResponse.notFound("Config not found");
        return ApiResponse.ok();
    }
}

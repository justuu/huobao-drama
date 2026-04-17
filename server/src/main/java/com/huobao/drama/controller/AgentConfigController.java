package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.AgentConfig;
import com.huobao.drama.service.AgentConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent-configs")
@RequiredArgsConstructor
public class AgentConfigController {

    private final AgentConfigService agentConfigService;

    @GetMapping
    public ApiResponse<List<AgentConfig>> listConfigs() {
        return ApiResponse.ok(agentConfigService.listActiveConfigs());
    }

    @GetMapping("/{id}")
    public ApiResponse<AgentConfig> getConfig(@PathVariable Long id) {
        AgentConfig config = agentConfigService.getConfig(id);
        if (config == null) return ApiResponse.notFound("AgentConfig not found");
        return ApiResponse.ok(config);
    }

    @PostMapping
    public ApiResponse<AgentConfig> upsertConfig(@RequestBody Map<String, Object> body) {
        AgentConfig config = agentConfigService.upsertConfig(body);
        return ApiResponse.created(config);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateConfig(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean found = agentConfigService.updateConfig(id, body);
        if (!found) return ApiResponse.notFound("AgentConfig not found");
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        boolean found = agentConfigService.softDelete(id);
        if (!found) return ApiResponse.notFound("AgentConfig not found");
        return ApiResponse.ok();
    }
}

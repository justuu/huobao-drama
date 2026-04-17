package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.exception.MissingConfigException;
import com.huobao.drama.service.agent.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/{type}/chat")
    public ApiResponse<Map<String, Object>> chat(
            @PathVariable String type,
            @RequestBody Map<String, Object> body) {
        if (!agentService.isValidType(type)) {
            return ApiResponse.badRequest("Invalid agent type: " + type);
        }
        Long episodeId = body.get("episode_id") instanceof Number ? ((Number) body.get("episode_id")).longValue() : null;
        Long dramaId = body.get("drama_id") instanceof Number ? ((Number) body.get("drama_id")).longValue() : null;
        String message = (String) body.getOrDefault("message", "");
        try {
            Map<String, Object> result = agentService.chat(type, episodeId, dramaId, message);
            return ApiResponse.ok(result);
        } catch (MissingConfigException e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    @GetMapping("/{type}/debug")
    public ApiResponse<Map<String, Object>> debug(@PathVariable String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agent_type", type);
        result.put("valid", agentService.isValidType(type));
        return ApiResponse.ok(result);
    }
}

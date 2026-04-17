package com.huobao.drama.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.AiVoice;
import com.huobao.drama.mapper.AiVoiceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-voices")
@RequiredArgsConstructor
public class AiVoiceController {

    private final AiVoiceMapper aiVoiceMapper;

    @GetMapping
    public ApiResponse<List<AiVoice>> listVoices(
            @RequestParam(value = "provider", defaultValue = "minimax") String provider) {
        return ApiResponse.ok(aiVoiceMapper.selectList(
                new LambdaQueryWrapper<AiVoice>().eq(AiVoice::getProvider, provider)));
    }

    @PostMapping("/sync")
    public ApiResponse<String> syncVoices() {
        return ApiResponse.ok("not implemented yet");
    }
}

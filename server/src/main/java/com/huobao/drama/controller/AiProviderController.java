package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.AiServiceProvider;
import com.huobao.drama.mapper.AiServiceProviderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai-providers")
@RequiredArgsConstructor
public class AiProviderController {

    private final AiServiceProviderMapper aiServiceProviderMapper;

    @GetMapping
    public ApiResponse<List<AiServiceProvider>> listProviders() {
        return ApiResponse.ok(aiServiceProviderMapper.selectList(null));
    }
}

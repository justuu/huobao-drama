package com.huobao.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.entity.AiServiceConfig;
import com.huobao.drama.entity.Character;
import com.huobao.drama.entity.ImageGeneration;
import com.huobao.drama.entity.Prop;
import com.huobao.drama.entity.Scene;
import com.huobao.drama.exception.MissingConfigException;
import com.huobao.drama.mapper.AiServiceConfigMapper;
import com.huobao.drama.mapper.CharacterMapper;
import com.huobao.drama.mapper.ImageGenerationMapper;
import com.huobao.drama.mapper.PropMapper;
import com.huobao.drama.mapper.SceneMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {

    private final AiServiceConfigMapper aiServiceConfigMapper;
    private final CharacterMapper characterMapper;
    private final SceneMapper sceneMapper;
    private final PropMapper propMapper;
    private final ImageGenerationMapper imageGenerationMapper;

    @Value("${storage.local-path}")
    private String storagePath;

    private final RestTemplate restTemplate = new RestTemplate();

    private AiServiceConfig resolveImageConfig() {
        List<AiServiceConfig> configs = aiServiceConfigMapper.selectList(
                new LambdaQueryWrapper<AiServiceConfig>()
                        .eq(AiServiceConfig::getServiceType, "image")
                        .eq(AiServiceConfig::getIsActive, true)
                        .orderByDesc(AiServiceConfig::getIsDefault)
                        .orderByAsc(AiServiceConfig::getPriority));
        if (configs == null || configs.isEmpty()) {
            throw new MissingConfigException("未找到可用的图片 AI 配置，请在设置页面添加并启用 AI 服务配置（service_type=image）");
        }
        return configs.get(0);
    }

    public ImageGeneration generateForCharacter(Long characterId, Map<String, Object> params) {
        Character character = characterMapper.selectById(characterId);
        if (character == null) {
            throw new IllegalArgumentException("角色不存在");
        }

        String prompt = (String) params.getOrDefault("prompt",
                character.getPrompt() != null ? character.getPrompt() : character.getDescription());
        ImageGeneration img = createImageGeneration(prompt, params);
        img.setCharacterId(characterId);
        img.setDramaId(character.getDramaId());
        img.setImageType("character");
        imageGenerationMapper.insert(img);

        executeGeneration(img);
        return img;
    }

    public ImageGeneration generateForScene(Long sceneId, Map<String, Object> params) {
        Scene scene = sceneMapper.selectById(sceneId);
        if (scene == null) {
            throw new IllegalArgumentException("场景不存在");
        }

        String prompt = (String) params.getOrDefault("prompt",
                scene.getPrompt() != null ? scene.getPrompt()
                        : String.format("%s, %s", scene.getLocation(), scene.getTime()));
        ImageGeneration img = createImageGeneration(prompt, params);
        img.setSceneId(sceneId);
        img.setDramaId(scene.getDramaId());
        img.setImageType("scene");
        imageGenerationMapper.insert(img);

        executeGeneration(img);
        return img;
    }

    public ImageGeneration generateForProp(Long propId, Map<String, Object> params) {
        Prop prop = propMapper.selectById(propId);
        if (prop == null) {
            throw new IllegalArgumentException("道具不存在");
        }

        String prompt = (String) params.getOrDefault("prompt",
                prop.getPrompt() != null ? prop.getPrompt() : prop.getDescription());
        ImageGeneration img = createImageGeneration(prompt, params);
        img.setPropId(propId);
        img.setDramaId(prop.getDramaId());
        img.setImageType("prop");
        imageGenerationMapper.insert(img);

        executeGeneration(img);
        return img;
    }

    private ImageGeneration createImageGeneration(String prompt, Map<String, Object> params) {
        AiServiceConfig config = resolveImageConfig();

        ImageGeneration img = new ImageGeneration();
        img.setPrompt(prompt);
        img.setProvider(config.getProvider());
        img.setModel((String) params.getOrDefault("model", config.getModel()));
        img.setSize((String) params.getOrDefault("size", "1024x1024"));
        img.setStatus("pending");

        if (params.containsKey("negative_prompt")) {
            img.setNegativePrompt((String) params.get("negative_prompt"));
        }
        if (params.containsKey("steps")) {
            img.setSteps(Integer.valueOf(params.get("steps").toString()));
        }
        if (params.containsKey("cfg_scale")) {
            img.setCfgScale(Double.valueOf(params.get("cfg_scale").toString()));
        }

        return img;
    }

    private void executeGeneration(ImageGeneration img) {
        new Thread(() -> {
            try {
                AiServiceConfig config = resolveImageConfig();
                String imageUrl = callImageApi(config, img);

                String localPath = downloadImage(imageUrl, img.getId());

                img.setImageUrl(imageUrl);
                img.setLocalPath(localPath);
                img.setStatus("completed");
                img.setCompletedAt(LocalDateTime.now());
                imageGenerationMapper.updateById(img);

            } catch (Exception e) {
                log.error("图片生成失败: {}", e.getMessage(), e);
                img.setStatus("failed");
                img.setErrorMsg(e.getMessage());
                imageGenerationMapper.updateById(img);
            }
        }).start();
    }

    private String callImageApi(AiServiceConfig config, ImageGeneration img) {
        String provider = config.getProvider().toLowerCase();

        if ("openai".equals(provider)) {
            return callOpenAI(config, img);
        } else if ("stability".equals(provider) || "stabilityai".equals(provider)) {
            return callStabilityAI(config, img);
        } else {
            throw new UnsupportedOperationException("不支持的图片提供商: " + provider);
        }
    }

    private String callOpenAI(AiServiceConfig config, ImageGeneration img) {
        String apiUrl = config.getBaseUrl() + "/v1/images/generations";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("model", img.getModel() != null ? img.getModel() : "dall-e-3");
        body.put("prompt", img.getPrompt());
        body.put("aspect_ratio", "16:9");
        body.put("n", 1);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
        return (String) data.get(0).get("url");
    }

    private String callStabilityAI(AiServiceConfig config, ImageGeneration img) {
        throw new UnsupportedOperationException("Stability AI 集成待实现");
    }

    private String downloadImage(String imageUrl, Long imageId) throws Exception {
        Path dir = Paths.get(storagePath, "images");
        Files.createDirectories(dir);

        String filename = imageId + "_" + System.currentTimeMillis() + ".png";
        Path filePath = dir.resolve(filename);

        ResponseEntity<byte[]> response = restTemplate.getForEntity(URI.create(imageUrl), byte[].class);
        Files.write(filePath, response.getBody());

        return filePath.toString();
    }
}
package com.huobao.drama.service.seedance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.config.SeedanceConfig;
import com.huobao.drama.entity.*;
import com.huobao.drama.mapper.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedanceVideoService {

    private final SeedanceConfig config;
    private final StoryboardMapper storyboardMapper;
    private final CharacterMapper characterMapper;
    private final SceneMapper sceneMapper;
    private final PropMapper propMapper;
    private final VideoGenerationMapper videoGenerationMapper;
    private final SeedanceAssetMapper seedanceAssetMapper;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final int MAX_DURATION = 15;
    private static final int MAX_PROMPT_CHARS = 800;
    private static final int MAX_IMAGES = 9;

    private static final Pattern ROLE_PATTERN = Pattern.compile("<role>(.*?)</role>");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("<location>(.*?)</location>");
    private static final Pattern PROP_PATTERN = Pattern.compile("<prop>(.*?)</prop>");

    /**
     * Generate video for a storyboard group
     */
    public VideoGeneration generateForGroup(String groupId, Long dramaId) {
        List<Storyboard> boards = storyboardMapper.selectList(
                new LambdaQueryWrapper<Storyboard>()
                        .eq(Storyboard::getGroupId, groupId)
                        .orderByAsc(Storyboard::getGroupIndex));

        if (boards.isEmpty()) {
            throw new RuntimeException("No storyboards found for group " + groupId);
        }

        StringBuilder fullPrompt = new StringBuilder();
        int totalDurationMs = 0;
        Set<String> roleNames = new LinkedHashSet<>();
        Set<String> locationNames = new LinkedHashSet<>();
        Set<String> propNames = new LinkedHashSet<>();

        for (Storyboard sb : boards) {
            String text = sb.getPromptText();
            if (text != null) {
                fullPrompt.append(text).append("\n");
                extractNames(text, ROLE_PATTERN, roleNames);
                extractNames(text, LOCATION_PATTERN, locationNames);
                extractNames(text, PROP_PATTERN, propNames);
            }
            if (sb.getDurationMs() != null) totalDurationMs += sb.getDurationMs();
        }

        int totalDurationSec = (int) Math.ceil(totalDurationMs / 1000.0);

        if (totalDurationSec < 5 || totalDurationSec > MAX_DURATION) {
            throw new RuntimeException("Total duration " + totalDurationSec + "s out of range [5-15]");
        }
        int totalImages = roleNames.size() + locationNames.size() + propNames.size();
        if (totalImages > MAX_IMAGES) {
            throw new RuntimeException("Too many reference images: " + totalImages + " (max " + MAX_IMAGES + ")");
        }

        Map<String, String> assetMapping = resolveAssets(dramaId, roleNames, locationNames, propNames);

        List<Map<String, Object>> content = new ArrayList<>();
        String processedText = fullPrompt.toString();
        int imageIndex = 1;
        List<String> orderedAssetIds = new ArrayList<>();

        for (String loc : locationNames) {
            String assetId = assetMapping.get("scene:" + loc);
            if (assetId != null) {
                processedText = processedText.replace("<location>" + loc + "</location>", "@图" + imageIndex + "（" + loc + "）");
                orderedAssetIds.add(assetId);
                imageIndex++;
            }
        }
        for (String role : roleNames) {
            String cleanName = role.contains("--") ? role.substring(0, role.indexOf("--")) : role;
            String assetId = assetMapping.get("character:" + cleanName);
            if (assetId != null) {
                processedText = processedText.replace("<role>" + role + "</role>", "@图" + imageIndex + "（" + cleanName + "）");
                orderedAssetIds.add(assetId);
                imageIndex++;
            }
        }
        for (String prop : propNames) {
            String assetId = assetMapping.get("prop:" + prop);
            if (assetId != null) {
                processedText = processedText.replace("<prop>" + prop + "</prop>", "@图" + imageIndex + "（" + prop + "）");
                orderedAssetIds.add(assetId);
                imageIndex++;
            }
        }

        if (processedText.length() > MAX_PROMPT_CHARS) {
            throw new RuntimeException("Prompt too long: " + processedText.length() + " chars (max " + MAX_PROMPT_CHARS + ")");
        }

        content.add(Map.of("type", "text", "text", processedText));
        for (String assetId : orderedAssetIds) {
            content.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", "asset://" + assetId),
                    "role", "reference_image"));
        }

        VideoGeneration vg = new VideoGeneration();
        vg.setDramaId(dramaId);
        vg.setStoryboardId(boards.get(0).getId());
        vg.setStoryboardGroupId(groupId);
        vg.setModel(config.getVideoModel());
        vg.setPromptText(processedText);
        vg.setDuration(totalDurationSec);
        vg.setAspectRatio("16:9");
        vg.setStatus("processing");
        try {
            vg.setAssetRefs(objectMapper.writeValueAsString(orderedAssetIds));
        } catch (Exception ignored) {}
        videoGenerationMapper.insert(vg);

        submitVideoGeneration(vg.getId(), content, totalDurationSec);

        return vg;
    }

    /**
     * Submit video generation to Seedance API
     */
    private void submitVideoGeneration(Long videoGenId, List<Map<String, Object>> content, int duration) {
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("model", config.getVideoModel());
            request.put("content", content);
            request.put("generate_audio", true);
            request.put("ratio", "16:9");
            request.put("duration", duration);
            request.put("watermark", false);

            String body = objectMapper.writeValueAsString(request);
            String url = config.getVideoBaseUrl() + "/contents/generations/tasks";

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getArkApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            VideoGeneration vg = videoGenerationMapper.selectById(videoGenId);
            if (json.has("id")) {
                vg.setSeedanceTaskId(json.get("id").asText());
                vg.setTaskId(json.get("id").asText());
            } else {
                vg.setStatus("failed");
                vg.setErrorMsg(response.body());
            }
            vg.setUpdatedAt(LocalDateTime.now());
            videoGenerationMapper.updateById(vg);

        } catch (Exception e) {
            log.error("Failed to submit video generation {}", videoGenId, e);
            VideoGeneration vg = videoGenerationMapper.selectById(videoGenId);
            if (vg != null) {
                vg.setStatus("failed");
                vg.setErrorMsg(e.getMessage());
                vg.setUpdatedAt(LocalDateTime.now());
                videoGenerationMapper.updateById(vg);
            }
        }
    }

    /**
     * Poll video generation status
     */
    public VideoGeneration pollStatus(Long videoGenId) {
        VideoGeneration vg = videoGenerationMapper.selectById(videoGenId);
        if (vg == null || vg.getSeedanceTaskId() == null) return vg;
        if ("completed".equals(vg.getStatus()) || "failed".equals(vg.getStatus())) return vg;

        try {
            String url = config.getVideoBaseUrl() + "/contents/generations/tasks/" + vg.getSeedanceTaskId();
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + config.getArkApiKey())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            String status = json.path("status").asText("");
            if ("succeeded".equalsIgnoreCase(status)) {
                vg.setStatus("completed");
                JsonNode contentNode = json.path("content");
                if (contentNode.isArray()) {
                    for (JsonNode item : contentNode) {
                        if ("video_url".equals(item.path("type").asText())) {
                            vg.setVideoUrl(item.path("video_url").path("url").asText());
                            break;
                        }
                    }
                }
                vg.setCompletedAt(LocalDateTime.now());
            } else if ("failed".equalsIgnoreCase(status)) {
                vg.setStatus("failed");
                vg.setErrorMsg(json.path("error").path("message").asText("Unknown error"));
            }

            vg.setUpdatedAt(LocalDateTime.now());
            videoGenerationMapper.updateById(vg);
        } catch (Exception e) {
            log.error("Failed to poll video generation status {}", videoGenId, e);
        }

        return vg;
    }

    private Map<String, String> resolveAssets(Long dramaId, Set<String> roleNames,
                                               Set<String> locationNames, Set<String> propNames) {
        Map<String, String> mapping = new HashMap<>();

        List<SeedanceAsset> assets = seedanceAssetMapper.selectList(
                new LambdaQueryWrapper<SeedanceAsset>()
                        .eq(SeedanceAsset::getDramaId, dramaId)
                        .eq(SeedanceAsset::getStatus, "ready"));

        for (SeedanceAsset a : assets) {
            mapping.put(a.getRefType() + ":" + a.getName(), a.getAssetId());
        }

        return mapping;
    }

    private void extractNames(String text, Pattern pattern, Set<String> names) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
    }
}

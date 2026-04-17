package com.huobao.drama.service.seedance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.config.SeedanceConfig;
import com.huobao.drama.entity.*;
import com.huobao.drama.entity.Character;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedanceAssetService {

    private final SeedanceConfig config;
    private final SeedanceAssetGroupMapper assetGroupMapper;
    private final SeedanceAssetMapper assetMapper;
    private final CharacterMapper characterMapper;
    private final SceneMapper sceneMapper;
    private final PropMapper propMapper;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Create or get existing asset group for a drama
     */
    public SeedanceAssetGroup getOrCreateGroup(Long dramaId, String groupName) {
        SeedanceAssetGroup existing = assetGroupMapper.selectOne(
                new LambdaQueryWrapper<SeedanceAssetGroup>()
                        .eq(SeedanceAssetGroup::getDramaId, dramaId));
        if (existing != null) return existing;

        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "Name", groupName,
                    "Description", "Assets for drama " + dramaId,
                    "GroupType", "AIGC"));

            String responseBody = callVolcApi("CreateAssetGroup", "2024-01-01", body);
            JsonNode json = objectMapper.readTree(responseBody);

            SeedanceAssetGroup group = new SeedanceAssetGroup();
            group.setDramaId(dramaId);
            group.setGroupName(groupName);
            group.setGroupType("AIGC");

            JsonNode result = json.path("Result");
            if (result.has("Id")) {
                group.setSeedanceGroupId(result.get("Id").asText());
                group.setStatus("ready");
            } else {
                group.setStatus("failed");
                log.error("CreateAssetGroup failed: {}", responseBody);
            }

            assetGroupMapper.insert(group);
            return group;
        } catch (Exception e) {
            log.error("Failed to create asset group for drama {}", dramaId, e);
            SeedanceAssetGroup group = new SeedanceAssetGroup();
            group.setDramaId(dramaId);
            group.setGroupName(groupName);
            group.setGroupType("AIGC");
            group.setStatus("failed");
            assetGroupMapper.insert(group);
            return group;
        }
    }

    /**
     * Upload a single asset (character/scene/prop image) to Seedance
     */
    public SeedanceAsset uploadAsset(Long dramaId, String refType, Long refId,
                                      String name, String imageUrl) {
        SeedanceAssetGroup group = assetGroupMapper.selectOne(
                new LambdaQueryWrapper<SeedanceAssetGroup>()
                        .eq(SeedanceAssetGroup::getDramaId, dramaId));
        if (group == null) {
            throw new RuntimeException("No asset group found for drama " + dramaId);
        }

        SeedanceAsset existing = assetMapper.selectOne(
                new LambdaQueryWrapper<SeedanceAsset>()
                        .eq(SeedanceAsset::getGroupId, group.getId())
                        .eq(SeedanceAsset::getName, name));
        if (existing != null) {
            return existing;
        }

        SeedanceAsset asset = new SeedanceAsset();
        asset.setGroupId(group.getId());
        asset.setDramaId(dramaId);
        asset.setRefType(refType);
        asset.setRefId(refId);
        asset.setName(name);
        asset.setImageUrl(imageUrl);
        asset.setStatus("uploading");
        assetMapper.insert(asset);

        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "AssetGroupId", group.getSeedanceGroupId(),
                    "Name", name,
                    "Type", "image",
                    "Url", imageUrl));

            String responseBody = callVolcApi("CreateAsset", "2024-01-01", body);
            JsonNode json = objectMapper.readTree(responseBody);

            JsonNode result = json.path("Result");
            if (result.has("AssetId")) {
                asset.setAssetId(result.get("AssetId").asText());
                asset.setStatus("ready");
            } else {
                asset.setStatus("failed");
                log.error("CreateAsset failed: {}", responseBody);
            }
        } catch (Exception e) {
            asset.setStatus("failed");
            log.error("Failed to upload asset {} for drama {}", name, dramaId, e);
        }

        asset.setUpdatedAt(LocalDateTime.now());
        assetMapper.updateById(asset);

        updateSourceEntityAssetId(refType, refId, asset.getAssetId(), group.getSeedanceGroupId());

        return asset;
    }

    /**
     * Upload all character/scene/prop images for a drama
     */
    public List<SeedanceAsset> uploadAllAssets(Long dramaId) {
        List<SeedanceAsset> results = new ArrayList<>();

        List<Character> chars = characterMapper.selectList(
                new LambdaQueryWrapper<Character>()
                        .eq(Character::getDramaId, dramaId)
                        .isNotNull(Character::getImageUrl));
        for (Character c : chars) {
            results.add(uploadAsset(dramaId, "character", c.getId(), c.getName(), c.getImageUrl()));
        }

        List<Scene> scenes = sceneMapper.selectList(
                new LambdaQueryWrapper<Scene>()
                        .eq(Scene::getDramaId, dramaId)
                        .isNotNull(Scene::getImageUrl));
        for (Scene s : scenes) {
            results.add(uploadAsset(dramaId, "scene", s.getId(), s.getLocation(), s.getImageUrl()));
        }

        List<Prop> props = propMapper.selectList(
                new LambdaQueryWrapper<Prop>()
                        .eq(Prop::getDramaId, dramaId)
                        .isNotNull(Prop::getImageUrl));
        for (Prop p : props) {
            results.add(uploadAsset(dramaId, "prop", p.getId(), p.getName(), p.getImageUrl()));
        }

        return results;
    }

    /**
     * Get all assets for a drama
     */
    public List<SeedanceAsset> getAssets(Long dramaId) {
        return assetMapper.selectList(
                new LambdaQueryWrapper<SeedanceAsset>()
                        .eq(SeedanceAsset::getDramaId, dramaId));
    }

    private void updateSourceEntityAssetId(String refType, Long refId, String assetId, String groupId) {
        if (assetId == null) return;
        switch (refType) {
            case "character" -> {
                Character c = characterMapper.selectById(refId);
                if (c != null) { c.setAssetId(assetId); c.setAssetGroupId(groupId); characterMapper.updateById(c); }
            }
            case "scene" -> {
                Scene s = sceneMapper.selectById(refId);
                if (s != null) { s.setAssetId(assetId); s.setAssetGroupId(groupId); sceneMapper.updateById(s); }
            }
            case "prop" -> {
                Prop p = propMapper.selectById(refId);
                if (p != null) { p.setAssetId(assetId); p.setAssetGroupId(groupId); propMapper.updateById(p); }
            }
        }
    }

    private String callVolcApi(String action, String version, String body) throws Exception {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("Action", action);
        queryParams.put("Version", version);

        byte[] bodyBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Map<String, String> headers = VolcSignUtil.sign("POST", config.getAssetEndpoint(), "/",
                queryParams, bodyBytes, config.getAccessKey(), config.getSecretKey(),
                config.getRegion(), config.getService());

        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (queryString.length() > 0) queryString.append("&");
            queryString.append(entry.getKey()).append("=").append(entry.getValue());
        }

        String url = "https://" + config.getAssetEndpoint() + "/?" + queryString;

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
        headers.forEach(reqBuilder::header);

        HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}

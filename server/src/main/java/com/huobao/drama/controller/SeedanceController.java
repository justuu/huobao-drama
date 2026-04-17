package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import com.huobao.drama.entity.SeedanceAsset;
import com.huobao.drama.entity.SeedanceAssetGroup;
import com.huobao.drama.service.seedance.SeedanceAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/seedance")
@RequiredArgsConstructor
public class SeedanceController {

    private final SeedanceAssetService assetService;

    @PostMapping("/asset-groups")
    public ApiResponse<SeedanceAssetGroup> createAssetGroup(@RequestBody Map<String, Object> body) {
        Long dramaId = ((Number) body.get("drama_id")).longValue();
        String groupName = (String) body.getOrDefault("group_name", "drama-" + dramaId);
        SeedanceAssetGroup group = assetService.getOrCreateGroup(dramaId, groupName);
        return ApiResponse.ok(group);
    }

    @PostMapping("/assets")
    public ApiResponse<SeedanceAsset> uploadAsset(@RequestBody Map<String, Object> body) {
        Long dramaId = ((Number) body.get("drama_id")).longValue();
        String refType = (String) body.get("ref_type");
        Long refId = ((Number) body.get("ref_id")).longValue();
        String name = (String) body.get("name");
        String imageUrl = (String) body.get("image_url");
        SeedanceAsset asset = assetService.uploadAsset(dramaId, refType, refId, name, imageUrl);
        return ApiResponse.ok(asset);
    }

    @PostMapping("/assets/upload-all")
    public ApiResponse<List<SeedanceAsset>> uploadAllAssets(@RequestBody Map<String, Object> body) {
        Long dramaId = ((Number) body.get("drama_id")).longValue();
        List<SeedanceAsset> assets = assetService.uploadAllAssets(dramaId);
        return ApiResponse.ok(assets);
    }

    @GetMapping("/assets")
    public ApiResponse<List<SeedanceAsset>> getAssets(@RequestParam("dramaId") Long dramaId) {
        return ApiResponse.ok(assetService.getAssets(dramaId));
    }
}

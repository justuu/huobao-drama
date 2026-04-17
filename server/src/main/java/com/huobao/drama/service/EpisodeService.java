package com.huobao.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huobao.drama.entity.Episode;
import com.huobao.drama.entity.EpisodeCharacter;
import com.huobao.drama.entity.EpisodeScene;
import com.huobao.drama.entity.Prop;
import com.huobao.drama.entity.Scene;
import com.huobao.drama.entity.Storyboard;
import com.huobao.drama.entity.StoryboardCharacter;
import com.huobao.drama.entity.VideoGeneration;
import com.huobao.drama.entity.Character;
import com.huobao.drama.mapper.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final EpisodeMapper episodeMapper;
    private final CharacterMapper characterMapper;
    private final SceneMapper sceneMapper;
    private final PropMapper propMapper;
    private final StoryboardMapper storyboardMapper;
    private final StoryboardCharacterMapper storyboardCharacterMapper;
    private final EpisodeCharacterMapper episodeCharacterMapper;
    private final EpisodeSceneMapper episodeSceneMapper;
    private final VideoGenerationMapper videoGenerationMapper;

    public Episode createEpisode(Long dramaId, Long imageConfigId, Long videoConfigId, Long audioConfigId, String title) {
        // Find next episode number
        LambdaQueryWrapper<Episode> wrapper = new LambdaQueryWrapper<Episode>()
                .eq(Episode::getDramaId, dramaId)
                .orderByDesc(Episode::getEpisodeNumber)
                .last("LIMIT 1");
        Episode last = episodeMapper.selectOne(wrapper);
        int nextNumber = (last == null || last.getEpisodeNumber() == null) ? 1 : last.getEpisodeNumber() + 1;

        Episode episode = new Episode();
        episode.setDramaId(dramaId);
        episode.setEpisodeNumber(nextNumber);
        episode.setTitle(title);
        episode.setImageConfigId(imageConfigId);
        episode.setVideoConfigId(videoConfigId);
        episode.setAudioConfigId(audioConfigId);
        episode.setStatus("pending");
        episodeMapper.insert(episode);
        return episode;
    }

    public boolean updateEpisode(Long id, Map<String, Object> fields) {
        Episode episode = episodeMapper.selectById(id);
        if (episode == null) return false;

        if (fields.containsKey("content")) episode.setContent((String) fields.get("content"));
        if (fields.containsKey("script_content")) episode.setScriptContent((String) fields.get("script_content"));
        if (fields.containsKey("title")) episode.setTitle((String) fields.get("title"));
        if (fields.containsKey("description")) episode.setDescription((String) fields.get("description"));
        if (fields.containsKey("status")) episode.setStatus((String) fields.get("status"));

        episodeMapper.updateById(episode);
        return true;
    }

    public List<com.huobao.drama.entity.Character> getEpisodeCharacters(Long episodeId) {
        List<EpisodeCharacter> links = episodeCharacterMapper.selectList(
                new LambdaQueryWrapper<EpisodeCharacter>().eq(EpisodeCharacter::getEpisodeId, episodeId));
        if (links.isEmpty()) return Collections.emptyList();
        List<Long> ids = links.stream().map(EpisodeCharacter::getCharacterId).collect(Collectors.toList());
        return characterMapper.selectBatchIds(ids);
    }

    public List<Scene> getEpisodeScenes(Long episodeId) {
        List<EpisodeScene> links = episodeSceneMapper.selectList(
                new LambdaQueryWrapper<EpisodeScene>().eq(EpisodeScene::getEpisodeId, episodeId));
        if (links.isEmpty()) return Collections.emptyList();
        List<Long> ids = links.stream().map(EpisodeScene::getSceneId).collect(Collectors.toList());
        return sceneMapper.selectBatchIds(ids);
    }

    public List<Map<String, Object>> getEpisodeStoryboards(Long episodeId) {
        List<Storyboard> storyboards = storyboardMapper.selectList(
                new LambdaQueryWrapper<Storyboard>()
                        .eq(Storyboard::getEpisodeId, episodeId)
                        .orderByAsc(Storyboard::getStoryboardNumber));

        if (storyboards.isEmpty()) return Collections.emptyList();

        List<Long> storyboardIds = storyboards.stream().map(Storyboard::getId).collect(Collectors.toList());

        // Fetch all storyboard-character links in one query
        List<StoryboardCharacter> allLinks = storyboardCharacterMapper.selectList(
                new LambdaQueryWrapper<StoryboardCharacter>().in(StoryboardCharacter::getStoryboardId, storyboardIds));

        // Collect unique character ids
        Set<Long> charIdSet = allLinks.stream().map(StoryboardCharacter::getCharacterId).collect(Collectors.toSet());
        Map<Long, com.huobao.drama.entity.Character> charMap = charIdSet.isEmpty() ? Collections.emptyMap() :
                characterMapper.selectBatchIds(charIdSet).stream().collect(Collectors.toMap(com.huobao.drama.entity.Character::getId, c -> c));

        // Group links by storyboard
        Map<Long, List<Long>> sbCharIds = new HashMap<>();
        for (StoryboardCharacter sc : allLinks) {
            sbCharIds.computeIfAbsent(sc.getStoryboardId(), k -> new ArrayList<>()).add(sc.getCharacterId());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Storyboard sb : storyboards) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", sb.getId());
            row.put("episodeId", sb.getEpisodeId());
            row.put("sceneId", sb.getSceneId());
            row.put("storyboardNumber", sb.getStoryboardNumber());
            row.put("title", sb.getTitle());
            row.put("location", sb.getLocation());
            row.put("time", sb.getTime());
            row.put("shotType", sb.getShotType());
            row.put("angle", sb.getAngle());
            row.put("movement", sb.getMovement());
            row.put("action", sb.getAction());
            row.put("result", sb.getResult());
            row.put("atmosphere", sb.getAtmosphere());
            row.put("videoPrompt", sb.getVideoPrompt());
            row.put("bgmPrompt", sb.getBgmPrompt());
            row.put("soundEffect", sb.getSoundEffect());
            row.put("dialogue", sb.getDialogue());
            row.put("description", sb.getDescription());
            row.put("duration", sb.getDuration());
            row.put("promptText", sb.getPromptText());
            row.put("durationMs", sb.getDurationMs());
            row.put("groupIndex", sb.getGroupIndex());
            row.put("groupId", sb.getGroupId());
            row.put("referenceImages", sb.getReferenceImages());
            row.put("videoUrl", sb.getVideoUrl());
            row.put("ttsAudioUrl", sb.getTtsAudioUrl());
            row.put("subtitleUrl", sb.getSubtitleUrl());
            row.put("composedVideoUrl", sb.getComposedVideoUrl());
            row.put("status", sb.getStatus());
            row.put("createdAt", sb.getCreatedAt());
            row.put("updatedAt", sb.getUpdatedAt());

            List<Long> cids = sbCharIds.getOrDefault(sb.getId(), Collections.emptyList());
            row.put("character_ids", cids);
            row.put("characters", cids.stream().map(charMap::get).filter(Objects::nonNull).collect(Collectors.toList()));
            result.add(row);
        }
        return result;
    }

    public Map<String, Object> getPipelineStatus(Long episodeId) {
        Episode episode = episodeMapper.selectById(episodeId);
        if (episode == null) return null;

        // Characters linked to episode
        List<EpisodeCharacter> epChars = episodeCharacterMapper.selectList(
                new LambdaQueryWrapper<EpisodeCharacter>().eq(EpisodeCharacter::getEpisodeId, episodeId));
        List<Long> charIds = epChars.stream().map(EpisodeCharacter::getCharacterId).collect(Collectors.toList());
        List<com.huobao.drama.entity.Character> characters = charIds.isEmpty() ? Collections.emptyList() : characterMapper.selectBatchIds(charIds);

        // Scenes linked to episode
        List<EpisodeScene> epScenes = episodeSceneMapper.selectList(
                new LambdaQueryWrapper<EpisodeScene>().eq(EpisodeScene::getEpisodeId, episodeId));
        List<Long> sceneIds = epScenes.stream().map(EpisodeScene::getSceneId).collect(Collectors.toList());
        List<Scene> scenes = sceneIds.isEmpty() ? Collections.emptyList() : sceneMapper.selectBatchIds(sceneIds);

        // Props for the drama
        List<Prop> props = propMapper.selectList(
                new LambdaQueryWrapper<Prop>().eq(Prop::getDramaId, episode.getDramaId()));

        // Storyboards for episode
        List<Storyboard> storyboards = storyboardMapper.selectList(
                new LambdaQueryWrapper<Storyboard>().eq(Storyboard::getEpisodeId, episodeId));

        // Video generations for episode's storyboards
        List<VideoGeneration> videoGens = Collections.emptyList();
        if (!storyboards.isEmpty()) {
            List<Long> sbIds = storyboards.stream().map(Storyboard::getId).collect(Collectors.toList());
            videoGens = videoGenerationMapper.selectList(
                    new LambdaQueryWrapper<VideoGeneration>().in(VideoGeneration::getStoryboardId, sbIds));
        }

        // --- compute steps ---
        Map<String, Object> steps = new LinkedHashMap<>();

        // script_rewrite
        String sc = episode.getScriptContent();
        steps.put("script_rewrite", Map.of("status", (sc != null && !sc.isEmpty()) ? "done" : "pending"));

        // extract_characters
        int charCount = characters.size();
        steps.put("extract_characters", Map.of("status", charCount > 0 ? "done" : "pending", "count", charCount));

        // extract_scenes
        int sceneCount = scenes.size();
        steps.put("extract_scenes", Map.of("status", sceneCount > 0 ? "done" : "pending", "count", sceneCount));

        // extract_props
        int propCount = props.size();
        steps.put("extract_props", Map.of("status", propCount > 0 ? "done" : "pending", "count", propCount));

        // generate_asset_images: chars+scenes+props with non-null imageUrl vs total
        int totalAssets = characters.size() + scenes.size() + props.size();
        long imagesCompleted = characters.stream().filter(c -> c.getImageUrl() != null).count()
                + scenes.stream().filter(s -> s.getImageUrl() != null).count()
                + props.stream().filter(p -> p.getImageUrl() != null).count();
        String imgStatus = totalAssets == 0 ? "pending" : (imagesCompleted >= totalAssets ? "done" : "partial");
        steps.put("generate_asset_images", Map.of("status", imgStatus, "completed", imagesCompleted, "total", totalAssets));

        // upload_assets: chars+scenes+props with non-null assetId vs those with imageUrl
        long withImage = characters.stream().filter(c -> c.getImageUrl() != null).count()
                + scenes.stream().filter(s -> s.getImageUrl() != null).count()
                + props.stream().filter(p -> p.getImageUrl() != null).count();
        long uploadedCount = characters.stream().filter(c -> c.getAssetId() != null).count()
                + scenes.stream().filter(s -> s.getAssetId() != null).count()
                + props.stream().filter(p -> p.getAssetId() != null).count();
        String uploadStatus = withImage == 0 ? "pending" : (uploadedCount >= withImage ? "done" : "partial");
        steps.put("upload_assets", Map.of("status", uploadStatus, "completed", uploadedCount, "total", withImage));

        // assign_voices: chars with non-null voiceSampleUrl vs total chars
        long voiceAssigned = characters.stream().filter(c -> c.getVoiceSampleUrl() != null).count();
        int totalChars = characters.size();
        String voiceStatus = totalChars == 0 ? "pending" : (voiceAssigned >= totalChars ? "done" : "partial");
        steps.put("assign_voices", Map.of("status", voiceStatus, "assigned", voiceAssigned, "total", totalChars));

        // storyboard_breaking
        int sbCount = storyboards.size();
        steps.put("storyboard_breaking", Map.of("status", sbCount > 0 ? "done" : "pending", "count", sbCount));

        // generate_videos: video_generations with status='completed' vs total storyboards
        long vgCompleted = videoGens.stream().filter(v -> "completed".equals(v.getStatus())).count();
        int totalSb = storyboards.size();
        String vgStatus = totalSb == 0 ? "pending" : (vgCompleted >= totalSb ? "done" : "partial");
        steps.put("generate_videos", Map.of("status", vgStatus, "completed", vgCompleted, "total", totalSb));

        // compose_merge: episode.videoUrl not null
        String mergeStatus = (episode.getVideoUrl() != null && !episode.getVideoUrl().isEmpty()) ? "done" : "pending";
        steps.put("compose_merge", Map.of("status", mergeStatus, "completed", mergeStatus.equals("done") ? 1 : 0, "total", 1));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episode_id", episodeId);
        result.put("steps", steps);
        return result;
    }
}

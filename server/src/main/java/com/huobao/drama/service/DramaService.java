package com.huobao.drama.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huobao.drama.entity.*;
import com.huobao.drama.entity.Character;
import com.huobao.drama.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DramaService {

    private final DramaMapper dramaMapper;
    private final EpisodeMapper episodeMapper;
    private final CharacterMapper characterMapper;
    private final SceneMapper sceneMapper;
    private final PropMapper propMapper;

    // ---- List with pagination ----

    public Map<String, Object> listDramas(int page, int pageSize, String status, String keyword) {
        LambdaQueryWrapper<Drama> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Drama::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Drama::getTitle, keyword).or().like(Drama::getDescription, keyword));
        }
        wrapper.orderByDesc(Drama::getCreatedAt);

        Page<Drama> pageResult = dramaMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<Drama> dramas = pageResult.getRecords();

        // Collect drama ids for batch count queries
        if (dramas.isEmpty()) {
            long total = pageResult.getTotal();
            long totalPages = (total + pageSize - 1) / pageSize;
            Map<String, Object> pagination = new LinkedHashMap<>();
            pagination.put("page", page);
            pagination.put("page_size", pageSize);
            pagination.put("total", total);
            pagination.put("total_pages", totalPages);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("items", Collections.emptyList());
            result.put("pagination", pagination);
            return result;
        }

        List<Long> dramaIds = dramas.stream().map(Drama::getId).toList();

        // Count episodes per drama
        Map<Long, Long> episodeCounts = countByDramaIds(episodeMapper, dramaIds,
                new LambdaQueryWrapper<Episode>().in(Episode::getDramaId, dramaIds));
        Map<Long, Long> characterCounts = countByDramaIds(characterMapper, dramaIds,
                new LambdaQueryWrapper<Character>().in(Character::getDramaId, dramaIds));
        Map<Long, Long> sceneCounts = countByDramaIds(sceneMapper, dramaIds,
                new LambdaQueryWrapper<Scene>().in(Scene::getDramaId, dramaIds));

        List<Map<String, Object>> items = new ArrayList<>();
        for (Drama d : dramas) {
            Map<String, Object> item = dramaToMap(d);
            item.put("episode_count", episodeCounts.getOrDefault(d.getId(), 0L));
            item.put("character_count", characterCounts.getOrDefault(d.getId(), 0L));
            item.put("scene_count", sceneCounts.getOrDefault(d.getId(), 0L));
            items.add(item);
        }

        long total = pageResult.getTotal();
        long totalPages = (total + pageSize - 1) / pageSize;
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", page);
        pagination.put("page_size", pageSize);
        pagination.put("total", total);
        pagination.put("total_pages", totalPages);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("pagination", pagination);
        return result;
    }

    // Helper: count grouped by dramaId using in-memory grouping after a list query
    private <T> Map<Long, Long> countByDramaIds(
            com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
            List<Long> dramaIds,
            LambdaQueryWrapper<T> wrapper) {
        // We just need counts; fetch all matching records and group
        // For large datasets a custom SQL would be better, but this keeps it simple
        List<T> records = mapper.selectList(wrapper);
        Map<Long, Long> counts = new HashMap<>();
        for (T record : records) {
            Long did = getDramaId(record);
            if (did != null) {
                counts.merge(did, 1L, Long::sum);
            }
        }
        return counts;
    }

    @SuppressWarnings("unchecked")
    private <T> Long getDramaId(T record) {
        try {
            return (Long) record.getClass().getMethod("getDramaId").invoke(record);
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Create ----

    @Transactional
    public Drama createDrama(Map<String, Object> body) {
        Drama drama = new Drama();
        drama.setTitle((String) body.get("title"));
        drama.setDescription((String) body.get("description"));
        drama.setGenre((String) body.get("genre"));
        drama.setStyle((String) body.get("style"));
        drama.setTags(body.get("tags") != null ? body.get("tags").toString() : null);
        drama.setMetadata(body.get("metadata") != null ? body.get("metadata").toString() : null);
        drama.setStatus("draft");
        Object te = body.get("total_episodes");
        int totalEpisodes = 0;
        if (te instanceof Number) {
            totalEpisodes = ((Number) te).intValue();
        }
        drama.setTotalEpisodes(totalEpisodes);
        dramaMapper.insert(drama);

        // Create default episodes
        for (int i = 1; i <= totalEpisodes; i++) {
            Episode ep = new Episode();
            ep.setDramaId(drama.getId());
            ep.setEpisodeNumber(i);
            ep.setTitle("第" + i + "集");
            ep.setStatus("draft");
            episodeMapper.insert(ep);
        }

        return drama;
    }

    // ---- Stats ----

    public Map<String, Object> getStats() {
        long total = dramaMapper.selectCount(new LambdaQueryWrapper<>());
        List<Drama> all = dramaMapper.selectList(
                new LambdaQueryWrapper<Drama>().select(Drama::getStatus));
        Map<String, Long> statusMap = new LinkedHashMap<>();
        for (Drama d : all) {
            String s = d.getStatus() != null ? d.getStatus() : "unknown";
            statusMap.merge(s, 1L, Long::sum);
        }
        List<Map<String, Object>> byStatus = new ArrayList<>();
        statusMap.forEach((s, c) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("status", s);
            entry.put("count", c);
            byStatus.add(entry);
        });
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("by_status", byStatus);
        return result;
    }

    // ---- Detail ----

    public Map<String, Object> getDramaDetail(Long id) {
        Drama drama = dramaMapper.selectById(id);
        if (drama == null) return null;

        Map<String, Object> detail = dramaToMap(drama);

        List<Episode> episodes = episodeMapper.selectList(
                new LambdaQueryWrapper<Episode>().eq(Episode::getDramaId, id).orderByAsc(Episode::getEpisodeNumber));
        List<Character> characters = characterMapper.selectList(
                new LambdaQueryWrapper<Character>().eq(Character::getDramaId, id));
        List<Scene> scenes = sceneMapper.selectList(
                new LambdaQueryWrapper<Scene>().eq(Scene::getDramaId, id));
        List<Prop> props = propMapper.selectList(
                new LambdaQueryWrapper<Prop>().eq(Prop::getDramaId, id));

        detail.put("episodes", episodes);
        detail.put("characters", characters);
        detail.put("scenes", scenes);
        detail.put("props", props);
        return detail;
    }

    // ---- Update ----

    public boolean updateDrama(Long id, Map<String, Object> body) {
        Drama drama = dramaMapper.selectById(id);
        if (drama == null) return false;

        if (body.containsKey("title")) drama.setTitle((String) body.get("title"));
        if (body.containsKey("description")) drama.setDescription((String) body.get("description"));
        if (body.containsKey("genre")) drama.setGenre((String) body.get("genre"));
        if (body.containsKey("style")) drama.setStyle((String) body.get("style"));
        if (body.containsKey("status")) drama.setStatus((String) body.get("status"));
        if (body.containsKey("tags")) drama.setTags(body.get("tags") != null ? body.get("tags").toString() : null);
        if (body.containsKey("metadata")) drama.setMetadata(body.get("metadata") != null ? body.get("metadata").toString() : null);

        dramaMapper.updateById(drama);
        return true;
    }

    // ---- Soft delete ----

    public boolean deleteDrama(Long id) {
        Drama drama = dramaMapper.selectById(id);
        if (drama == null) return false;
        dramaMapper.deleteById(id);
        return true;
    }

    // ---- Batch save characters ----

    @Transactional
    public void batchSaveCharacters(Long dramaId, List<Map<String, Object>> characters) {
        for (Map<String, Object> c : characters) {
            String name = (String) c.get("name");
            LambdaQueryWrapper<Character> wrapper = new LambdaQueryWrapper<Character>()
                    .eq(Character::getDramaId, dramaId)
                    .eq(Character::getName, name);
            Character existing = characterMapper.selectOne(wrapper);
            if (existing == null) {
                existing = new Character();
                existing.setDramaId(dramaId);
                existing.setName(name);
            }
            if (c.containsKey("role")) existing.setRole((String) c.get("role"));
            if (c.containsKey("description")) existing.setDescription((String) c.get("description"));
            if (c.containsKey("appearance")) existing.setAppearance((String) c.get("appearance"));
            if (c.containsKey("personality")) existing.setPersonality((String) c.get("personality"));
            if (c.containsKey("voice_style")) existing.setVoiceStyle((String) c.get("voice_style"));
            if (existing.getId() == null) {
                characterMapper.insert(existing);
            } else {
                characterMapper.updateById(existing);
            }
        }
    }

    // ---- Batch save episodes ----

    @Transactional
    public void batchSaveEpisodes(Long dramaId, List<Map<String, Object>> episodes) {
        for (Map<String, Object> e : episodes) {
            Object epNumObj = e.get("episode_number");
            if (epNumObj == null) continue;
            int epNum = ((Number) epNumObj).intValue();
            LambdaQueryWrapper<Episode> wrapper = new LambdaQueryWrapper<Episode>()
                    .eq(Episode::getDramaId, dramaId)
                    .eq(Episode::getEpisodeNumber, epNum);
            Episode existing = episodeMapper.selectOne(wrapper);
            if (existing == null) {
                existing = new Episode();
                existing.setDramaId(dramaId);
                existing.setEpisodeNumber(epNum);
            }
            if (e.containsKey("title")) existing.setTitle((String) e.get("title"));
            if (e.containsKey("description")) existing.setDescription((String) e.get("description"));
            if (existing.getId() == null) {
                episodeMapper.insert(existing);
            } else {
                episodeMapper.updateById(existing);
            }
        }
    }

    // ---- Helpers ----

    private Map<String, Object> dramaToMap(Drama d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("title", d.getTitle());
        m.put("description", d.getDescription());
        m.put("genre", d.getGenre());
        m.put("style", d.getStyle());
        m.put("total_episodes", d.getTotalEpisodes());
        m.put("total_duration", d.getTotalDuration());
        m.put("status", d.getStatus());
        m.put("thumbnail", d.getThumbnail());
        m.put("tags", d.getTags());
        m.put("metadata", d.getMetadata());
        m.put("created_at", d.getCreatedAt());
        m.put("updated_at", d.getUpdatedAt());
        return m;
    }
}

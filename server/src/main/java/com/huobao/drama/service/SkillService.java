package com.huobao.drama.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Service
public class SkillService {

    @Value("${skills.path}")
    private String skillsPath;

    private Path getSkillsDir() {
        return Paths.get(skillsPath).toAbsolutePath().normalize();
    }

    private Path getSkillFile(String id) {
        return getSkillsDir().resolve(id).resolve("SKILL.md");
    }

    public List<Map<String, String>> listSkills() throws IOException {
        Path dir = getSkillsDir();
        if (!Files.exists(dir)) return Collections.emptyList();

        List<Map<String, String>> result = new ArrayList<>();
        try (Stream<Path> entries = Files.list(dir)) {
            entries.filter(Files::isDirectory).forEach(subDir -> {
                Path skillFile = subDir.resolve("SKILL.md");
                if (Files.exists(skillFile)) {
                    try {
                        String content = Files.readString(skillFile);
                        Map<String, String> frontmatter = parseFrontmatter(content);
                        Map<String, String> skill = new LinkedHashMap<>();
                        skill.put("id", subDir.getFileName().toString());
                        skill.put("name", frontmatter.getOrDefault("name", subDir.getFileName().toString()));
                        skill.put("description", frontmatter.getOrDefault("description", ""));
                        result.add(skill);
                    } catch (IOException ignored) {}
                }
            });
        }
        return result;
    }

    public Map<String, String> getSkill(String id) throws IOException {
        Path file = getSkillFile(id);
        if (!Files.exists(file)) return null;
        String content = Files.readString(file);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("content", content);
        return result;
    }

    public void updateSkill(String id, String content) throws IOException {
        Path file = getSkillFile(id);
        Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public Map<String, String> createSkill(String id, String name, String description) throws IOException {
        Path dir = getSkillsDir().resolve(id);
        Files.createDirectories(dir);
        String content = "---\nname: " + name + "\ndescription: " + description + "\n---\n";
        Files.writeString(dir.resolve("SKILL.md"), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("description", description);
        return result;
    }

    public boolean deleteSkill(String id) throws IOException {
        Path dir = getSkillsDir().resolve(id);
        if (!Files.exists(dir)) return false;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
        }
        return true;
    }

    private Map<String, String> parseFrontmatter(String content) {
        Map<String, String> map = new HashMap<>();
        if (!content.startsWith("---")) return map;
        int end = content.indexOf("---", 3);
        if (end < 0) return map;
        String block = content.substring(3, end).trim();
        for (String line : block.split("\n")) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                map.put(key, value);
            }
        }
        return map;
    }
}

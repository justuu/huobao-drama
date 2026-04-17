package com.huobao.drama.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class SkillLoader {

    @Value("${skills.path:../skills}")
    private String skillsPath;

    public String load(String skillId) {
        String content = loadFromClasspath(skillId);
        if (content == null) {
            content = loadFromFilesystem(skillId);
        }
        if (content == null) {
            log.warn("Skill file not found for skillId: {}", skillId);
            return "";
        }
        return stripFrontmatter(content);
    }

    private String loadFromClasspath(String skillId) {
        String path = "skills/" + skillId + "/SKILL.md";
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) return null;
        try (InputStream is = resource.getInputStream()) {
            log.info("Loading skill from classpath: {}", path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private String loadFromFilesystem(String skillId) {
        Path file = Path.of(skillsPath, skillId, "SKILL.md").toAbsolutePath().normalize();
        if (!Files.exists(file)) return null;
        try {
            log.info("Loading skill from filesystem: {}", file);
            return Files.readString(file);
        } catch (IOException e) {
            log.error("Failed to read skill file: {}", file, e);
            return null;
        }
    }

    private String stripFrontmatter(String content) {
        if (content.startsWith("---")) {
            int end = content.indexOf("---", 3);
            if (end > 0) return content.substring(end + 3).trim();
        }
        return content;
    }
}

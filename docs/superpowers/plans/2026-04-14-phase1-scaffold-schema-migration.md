# Phase 1: Spring Boot Scaffold + MySQL Schema + Data Migration

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a bootable Spring Boot 3 application with MySQL 8, all entity/mapper layers, Flyway schema migration, and a SQLite→MySQL data migration script.

**Architecture:** Standard Spring Boot layered architecture with MyBatis-Plus for ORM. Flyway manages schema versioning. A standalone Java main class handles one-time SQLite→MySQL data migration. The app boots, connects to MySQL, and serves a health endpoint.

**Tech Stack:** Java 21, Spring Boot 3.3, MyBatis-Plus 3.5, Flyway, MySQL Connector/J, SQLite JDBC (migration only)

**Spec:** `docs/superpowers/specs/2026-04-14-java-backend-seedance-redesign.md`

**Phases Overview:**
- **Phase 1 (this plan):** Project scaffold + MySQL schema + entities + data migration
- **Phase 2:** Core CRUD controllers (dramas, episodes, characters, scenes, props, configs, skills)
- **Phase 3:** Agent system (Spring AI + Skills + Tools) + Seedance 2.0 integration
- **Phase 4:** Frontend storyboard editor + pipeline status updates

---

### Task 1: Initialize Spring Boot Project with Maven

**Files:**
- Create: `server/pom.xml`
- Create: `server/src/main/java/com/huobao/drama/HuobaoDramaApplication.java`
- Create: `server/src/main/resources/application.yml`
- Create: `server/src/main/resources/application-dev.yml`

The Java backend lives in `server/` (sibling to existing `backend/`, `frontend/`).

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.6</version>
    </parent>
    <groupId>com.huobao</groupId>
    <artifactId>drama-server</artifactId>
    <version>1.0.0</version>
    <name>huobao-drama-server</name>

    <properties>
        <java.version>21</java.version>
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <spring-ai.version>1.0.0</spring-ai.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Flyway -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Jackson for JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create application entry point**

Create `server/src/main/java/com/huobao/drama/HuobaoDramaApplication.java`:

```java
package com.huobao.drama;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HuobaoDramaApplication {
    public static void main(String[] args) {
        SpringApplication.run(HuobaoDramaApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application.yml**

Create `server/src/main/resources/application.yml`:

```yaml
server:
  port: 5679

spring:
  application:
    name: huobao-drama-server
  profiles:
    active: dev
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# Storage
storage:
  type: local
  local-path: ../data/storage
  base-url: http://localhost:5679/static

# Skills
skills:
  path: ../skills
```

- [ ] **Step 4: Create application-dev.yml**

Create `server/src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/huobao_drama?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
```

- [ ] **Step 5: Verify project compiles**

Run: `cd server && mvn compile -q`
Expected: BUILD SUCCESS (no source files yet beyond Application.java, but structure is valid)

- [ ] **Step 6: Commit**

```bash
git add server/pom.xml server/src/
git commit -m "feat: initialize Spring Boot 3 project scaffold"
```

---

### Task 2: Flyway Migration — Complete MySQL Schema

**Files:**
- Create: `server/src/main/resources/db/migration/V1__init_schema.sql`

This single migration creates ALL tables — both existing (adapted from SQLite) and new (Seedance). The schema matches the spec section 4.

- [ ] **Step 1: Create V1__init_schema.sql**

Create `server/src/main/resources/db/migration/V1__init_schema.sql`:

```sql
-- =============================================
-- Huobao Drama — MySQL Schema
-- Migrated from SQLite + Seedance 2.0 additions
-- =============================================

CREATE TABLE dramas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    genre VARCHAR(64),
    style VARCHAR(64) DEFAULT 'realistic',
    total_episodes INT DEFAULT 1,
    total_duration INT DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    thumbnail VARCHAR(512),
    tags TEXT,
    metadata TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE episodes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drama_id BIGINT NOT NULL,
    episode_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content MEDIUMTEXT,
    script_content MEDIUMTEXT,
    description TEXT,
    duration INT DEFAULT 0,
    status VARCHAR(32) DEFAULT 'draft',
    video_url VARCHAR(512),
    thumbnail VARCHAR(512),
    image_config_id BIGINT,
    video_config_id BIGINT,
    audio_config_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME,
    INDEX idx_episodes_drama_id (drama_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE characters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drama_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    role VARCHAR(64),
    description TEXT,
    appearance TEXT,
    personality TEXT,
    voice_style VARCHAR(128),
    image_url VARCHAR(512),
    reference_images TEXT,
    seed_value VARCHAR(64),
    sort_order INT,
    local_path VARCHAR(512),
    voice_sample_url VARCHAR(512),
    voice_provider VARCHAR(64),
    asset_id VARCHAR(128),
    asset_group_id VARCHAR(128),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME,
    INDEX idx_characters_drama_id (drama_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE episode_characters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    episode_id BIGINT NOT NULL,
    character_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_ec_episode_id (episode_id),
    INDEX idx_ec_character_id (character_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE episode_scenes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    episode_id BIGINT NOT NULL,
    scene_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_es_episode_id (episode_id),
    INDEX idx_es_scene_id (scene_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE scenes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drama_id BIGINT NOT NULL,
    episode_id BIGINT,
    location VARCHAR(255) NOT NULL,
    time VARCHAR(64) NOT NULL,
    prompt TEXT NOT NULL,
    storyboard_count INT DEFAULT 1,
    image_url VARCHAR(512),
    status VARCHAR(32) DEFAULT 'pending',
    local_path VARCHAR(512),
    asset_id VARCHAR(128),
    asset_group_id VARCHAR(128),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME,
    INDEX idx_scenes_drama_id (drama_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE storyboards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    episode_id BIGINT NOT NULL,
    scene_id BIGINT,
    storyboard_number INT NOT NULL,
    title VARCHAR(255),
    location VARCHAR(255),
    time VARCHAR(64),
    shot_type VARCHAR(64),
    angle VARCHAR(64),
    movement VARCHAR(128),
    action TEXT,
    result TEXT,
    atmosphere TEXT,
    video_prompt TEXT,
    bgm_prompt TEXT,
    sound_effect TEXT,
    dialogue TEXT,
    description TEXT,
    duration INT DEFAULT 0,
    prompt_text MEDIUMTEXT,
    duration_ms INT,
    group_index INT,
    group_id VARCHAR(64),
    reference_images TEXT,
    video_url VARCHAR(512),
    tts_audio_url VARCHAR(512),
    subtitle_url VARCHAR(512),
    composed_video_url VARCHAR(512),
    status VARCHAR(32) DEFAULT 'pending',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME,
    INDEX idx_storyboards_episode_id (episode_id),
    INDEX idx_storyboards_scene_id (scene_id),
    INDEX idx_storyboards_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE storyboard_characters (
    storyboard_id BIGINT NOT NULL,
    character_id BIGINT NOT NULL,
    PRIMARY KEY (storyboard_id, character_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_service_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_type VARCHAR(32) NOT NULL,
    provider VARCHAR(64),
    name VARCHAR(128) NOT NULL,
    base_url VARCHAR(512) NOT NULL,
    api_key VARCHAR(512) NOT NULL,
    model VARCHAR(128),
    endpoint VARCHAR(512),
    query_endpoint VARCHAR(512),
    priority INT DEFAULT 0,
    is_default TINYINT(1) DEFAULT 0,
    is_active TINYINT(1) DEFAULT 1,
    settings TEXT,
    access_key VARCHAR(256),
    secret_key VARCHAR(256),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_service_providers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    display_name VARCHAR(128),
    service_type VARCHAR(32) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    default_url VARCHAR(512),
    preset_models TEXT,
    description TEXT,
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_voices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    voice_id VARCHAR(128) NOT NULL UNIQUE,
    voice_name VARCHAR(128) NOT NULL,
    description TEXT,
    language VARCHAR(32),
    provider VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE agent_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_type VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    model VARCHAR(128),
    system_prompt MEDIUMTEXT,
    temperature DOUBLE,
    max_tokens INT,
    max_iterations INT,
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE image_generations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    storyboard_id BIGINT,
    drama_id BIGINT,
    scene_id BIGINT,
    character_id BIGINT,
    prop_id BIGINT,
    image_type VARCHAR(32),
    frame_type VARCHAR(32),
    provider VARCHAR(64),
    prompt TEXT,
    negative_prompt TEXT,
    model VARCHAR(128),
    size VARCHAR(32),
    quality VARCHAR(32),
    style VARCHAR(64),
    steps INT,
    cfg_scale DOUBLE,
    seed BIGINT,
    image_url VARCHAR(512),
    minio_url VARCHAR(512),
    local_path VARCHAR(512),
    status VARCHAR(32) DEFAULT 'pending',
    task_id VARCHAR(128),
    error_msg TEXT,
    width INT,
    height INT,
    reference_images TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    completed_at DATETIME,
    INDEX idx_ig_storyboard_id (storyboard_id),
    INDEX idx_ig_drama_id (drama_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE video_generations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    storyboard_id BIGINT,
    drama_id BIGINT,
    provider VARCHAR(64),
    prompt TEXT,
    model VARCHAR(128),
    duration INT,
    fps INT,
    resolution VARCHAR(32),
    aspect_ratio VARCHAR(16),
    style VARCHAR(64),
    motion_level INT,
    camera_motion VARCHAR(128),
    seed BIGINT,
    video_url VARCHAR(512),
    minio_url VARCHAR(512),
    local_path VARCHAR(512),
    status VARCHAR(32) DEFAULT 'pending',
    task_id VARCHAR(128),
    error_msg TEXT,
    width INT,
    height INT,
    seedance_task_id VARCHAR(128),
    asset_refs TEXT,
    storyboard_group_id VARCHAR(64),
    prompt_text MEDIUMTEXT,
    reference_image_urls TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    completed_at DATETIME,
    deleted_at DATETIME,
    INDEX idx_vg_storyboard_id (storyboard_id),
    INDEX idx_vg_drama_id (drama_id),
    INDEX idx_vg_group_id (storyboard_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE video_merges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    episode_id BIGINT,
    drama_id BIGINT,
    title VARCHAR(255),
    provider VARCHAR(64),
    model VARCHAR(128),
    status VARCHAR(32) DEFAULT 'pending',
    scenes TEXT,
    merged_url VARCHAR(512),
    duration INT,
    task_id VARCHAR(128),
    error_msg TEXT,
    created_at DATETIME NOT NULL,
    completed_at DATETIME,
    deleted_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE props (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drama_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(64),
    description TEXT,
    prompt TEXT,
    image_url VARCHAR(512),
    reference_images TEXT,
    local_path VARCHAR(512),
    asset_id VARCHAR(128),
    asset_group_id VARCHAR(128),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME,
    INDEX idx_props_drama_id (drama_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drama_id BIGINT,
    episode_id BIGINT,
    storyboard_id BIGINT,
    storyboard_num INT,
    name VARCHAR(255),
    description TEXT,
    type VARCHAR(32),
    category VARCHAR(64),
    url VARCHAR(512),
    thumbnail_url VARCHAR(512),
    local_path VARCHAR(512),
    file_size BIGINT,
    mime_type VARCHAR(128),
    width INT,
    height INT,
    duration INT,
    format VARCHAR(32),
    image_gen_id BIGINT,
    video_gen_id BIGINT,
    is_favorite TINYINT(1) DEFAULT 0,
    view_count INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seedance 2.0 tables

CREATE TABLE seedance_asset_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drama_id BIGINT NOT NULL,
    group_name VARCHAR(128),
    group_type VARCHAR(32) DEFAULT 'AIGC',
    seedance_group_id VARCHAR(128),
    status VARCHAR(32) DEFAULT 'pending',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_sag_drama_id (drama_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE seedance_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    drama_id BIGINT NOT NULL,
    ref_type VARCHAR(32) NOT NULL,
    ref_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    asset_id VARCHAR(128),
    image_url VARCHAR(512),
    status VARCHAR(32) DEFAULT 'pending',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_group_name (group_id, name),
    INDEX idx_sa_drama_id (drama_id),
    INDEX idx_sa_ref (ref_type, ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Verify Flyway migration syntax**

Run: `cd server && mvn compile -q`
Expected: BUILD SUCCESS (Flyway will run on first app start)

- [ ] **Step 3: Commit**

```bash
git add server/src/main/resources/db/migration/V1__init_schema.sql
git commit -m "feat: add Flyway V1 migration with complete MySQL schema"
```

---

### Task 3: MyBatis-Plus Entity Classes — Core Tables

**Files:**
- Create: `server/src/main/java/com/huobao/drama/entity/Drama.java`
- Create: `server/src/main/java/com/huobao/drama/entity/Episode.java`
- Create: `server/src/main/java/com/huobao/drama/entity/Character.java`
- Create: `server/src/main/java/com/huobao/drama/entity/Scene.java`
- Create: `server/src/main/java/com/huobao/drama/entity/Storyboard.java`
- Create: `server/src/main/java/com/huobao/drama/entity/StoryboardCharacter.java`
- Create: `server/src/main/java/com/huobao/drama/entity/Prop.java`

- [ ] **Step 1: Create Drama entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dramas")
public class Drama {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String genre;
    private String style;
    private Integer totalEpisodes;
    private Integer totalDuration;
    private String status;
    private String thumbnail;
    private String tags;
    private String metadata;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 2: Create Episode entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("episodes")
public class Episode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dramaId;
    private Integer episodeNumber;
    private String title;
    private String content;
    private String scriptContent;
    private String description;
    private Integer duration;
    private String status;
    private String videoUrl;
    private String thumbnail;
    private Long imageConfigId;
    private Long videoConfigId;
    private Long audioConfigId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 3: Create Character entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("characters")
public class Character {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dramaId;
    private String name;
    private String role;
    private String description;
    private String appearance;
    private String personality;
    private String voiceStyle;
    private String imageUrl;
    private String referenceImages;
    private String seedValue;
    private Integer sortOrder;
    private String localPath;
    private String voiceSampleUrl;
    private String voiceProvider;
    private String assetId;
    private String assetGroupId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 4: Create Scene entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("scenes")
public class Scene {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dramaId;
    private Long episodeId;
    private String location;
    private String time;
    private String prompt;
    private Integer storyboardCount;
    private String imageUrl;
    private String status;
    private String localPath;
    private String assetId;
    private String assetGroupId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 5: Create Storyboard entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("storyboards")
public class Storyboard {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long episodeId;
    private Long sceneId;
    private Integer storyboardNumber;
    private String title;
    private String location;
    private String time;
    private String shotType;
    private String angle;
    private String movement;
    private String action;
    private String result;
    private String atmosphere;
    private String videoPrompt;
    private String bgmPrompt;
    private String soundEffect;
    private String dialogue;
    private String description;
    private Integer duration;
    private String promptText;
    private Integer durationMs;
    private Integer groupIndex;
    private String groupId;
    private String referenceImages;
    private String videoUrl;
    private String ttsAudioUrl;
    private String subtitleUrl;
    private String composedVideoUrl;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 6: Create StoryboardCharacter entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("storyboard_characters")
public class StoryboardCharacter {
    private Long storyboardId;
    private Long characterId;
}
```

- [ ] **Step 7: Create Prop entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("props")
public class Prop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dramaId;
    private String name;
    private String type;
    private String description;
    private String prompt;
    private String imageUrl;
    private String referenceImages;
    private String localPath;
    private String assetId;
    private String assetGroupId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 8: Verify compilation**

Run: `cd server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add server/src/main/java/com/huobao/drama/entity/
git commit -m "feat: add MyBatis-Plus entity classes for core tables"
```

---

### Task 4: MyBatis-Plus Entity Classes — Config, Generation, Seedance Tables

**Files:**
- Create: `server/src/main/java/com/huobao/drama/entity/AiServiceConfig.java`
- Create: `server/src/main/java/com/huobao/drama/entity/AiServiceProvider.java`
- Create: `server/src/main/java/com/huobao/drama/entity/AiVoice.java`
- Create: `server/src/main/java/com/huobao/drama/entity/AgentConfig.java`
- Create: `server/src/main/java/com/huobao/drama/entity/ImageGeneration.java`
- Create: `server/src/main/java/com/huobao/drama/entity/VideoGeneration.java`
- Create: `server/src/main/java/com/huobao/drama/entity/VideoMerge.java`
- Create: `server/src/main/java/com/huobao/drama/entity/Asset.java`
- Create: `server/src/main/java/com/huobao/drama/entity/EpisodeCharacter.java`
- Create: `server/src/main/java/com/huobao/drama/entity/EpisodeScene.java`
- Create: `server/src/main/java/com/huobao/drama/entity/SeedanceAssetGroup.java`
- Create: `server/src/main/java/com/huobao/drama/entity/SeedanceAsset.java`

- [ ] **Step 1: Create AiServiceConfig entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_service_configs")
public class AiServiceConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String serviceType;
    private String provider;
    private String name;
    private String baseUrl;
    private String apiKey;
    private String model;
    private String endpoint;
    private String queryEndpoint;
    private Integer priority;
    private Boolean isDefault;
    private Boolean isActive;
    private String settings;
    private String accessKey;
    private String secretKey;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: Create AiServiceProvider entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_service_providers")
public class AiServiceProvider {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String displayName;
    private String serviceType;
    private String provider;
    private String defaultUrl;
    private String presetModels;
    private String description;
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: Create AiVoice entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_voices")
public class AiVoice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String voiceId;
    private String voiceName;
    private String description;
    private String language;
    private String provider;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: Create AgentConfig entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("agent_configs")
public class AgentConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String agentType;
    private String name;
    private String description;
    private String model;
    private String systemPrompt;
    private Double temperature;
    private Integer maxTokens;
    private Integer maxIterations;
    private Boolean isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 5: Create ImageGeneration entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("image_generations")
public class ImageGeneration {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storyboardId;
    private Long dramaId;
    private Long sceneId;
    private Long characterId;
    private Long propId;
    private String imageType;
    private String frameType;
    private String provider;
    private String prompt;
    private String negativePrompt;
    private String model;
    private String size;
    private String quality;
    private String style;
    private Integer steps;
    private Double cfgScale;
    private Long seed;
    private String imageUrl;
    private String minioUrl;
    private String localPath;
    private String status;
    private String taskId;
    private String errorMsg;
    private Integer width;
    private Integer height;
    private String referenceImages;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
```

- [ ] **Step 6: Create VideoGeneration entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_generations")
public class VideoGeneration {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storyboardId;
    private Long dramaId;
    private String provider;
    private String prompt;
    private String model;
    private Integer duration;
    private Integer fps;
    private String resolution;
    private String aspectRatio;
    private String style;
    private Integer motionLevel;
    private String cameraMotion;
    private Long seed;
    private String videoUrl;
    private String minioUrl;
    private String localPath;
    private String status;
    private String taskId;
    private String errorMsg;
    private Integer width;
    private Integer height;
    private String seedanceTaskId;
    private String assetRefs;
    private String storyboardGroupId;
    private String promptText;
    private String referenceImageUrls;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 7: Create VideoMerge, Asset, EpisodeCharacter, EpisodeScene entities**

VideoMerge:
```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_merges")
public class VideoMerge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long episodeId;
    private Long dramaId;
    private String title;
    private String provider;
    private String model;
    private String status;
    private String scenes;
    private String mergedUrl;
    private Integer duration;
    private String taskId;
    private String errorMsg;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}
```

Asset:
```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("assets")
public class Asset {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dramaId;
    private Long episodeId;
    private Long storyboardId;
    private Integer storyboardNum;
    private String name;
    private String description;
    private String type;
    private String category;
    private String url;
    private String thumbnailUrl;
    private String localPath;
    private Long fileSize;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Integer duration;
    private String format;
    private Long imageGenId;
    private Long videoGenId;
    private Boolean isFavorite;
    private Integer viewCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}
```

EpisodeCharacter:
```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("episode_characters")
public class EpisodeCharacter {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long episodeId;
    private Long characterId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

EpisodeScene:
```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("episode_scenes")
public class EpisodeScene {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long episodeId;
    private Long sceneId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 8: Create SeedanceAssetGroup entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seedance_asset_groups")
public class SeedanceAssetGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dramaId;
    private String groupName;
    private String groupType;
    private String seedanceGroupId;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 9: Create SeedanceAsset entity**

```java
package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seedance_assets")
public class SeedanceAsset {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long dramaId;
    private String refType;
    private Long refId;
    private String name;
    private String assetId;
    private String imageUrl;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 10: Verify compilation**

Run: `cd server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 11: Commit**

```bash
git add server/src/main/java/com/huobao/drama/entity/
git commit -m "feat: add MyBatis-Plus entity classes for config, generation, and seedance tables"
```

---

### Task 5: MyBatis-Plus Mapper Interfaces + Config

**Files:**
- Create: `server/src/main/java/com/huobao/drama/mapper/DramaMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/EpisodeMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/CharacterMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/SceneMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/StoryboardMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/StoryboardCharacterMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/PropMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/EpisodeCharacterMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/EpisodeSceneMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/AiServiceConfigMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/AiServiceProviderMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/AiVoiceMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/AgentConfigMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/ImageGenerationMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/VideoGenerationMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/VideoMergeMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/AssetMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/SeedanceAssetGroupMapper.java`
- Create: `server/src/main/java/com/huobao/drama/mapper/SeedanceAssetMapper.java`
- Create: `server/src/main/java/com/huobao/drama/config/MyBatisPlusConfig.java`

- [ ] **Step 1: Create all mapper interfaces**

Each mapper extends `BaseMapper<Entity>`. Example pattern (repeat for all entities):

```java
package com.huobao.drama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huobao.drama.entity.Drama;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DramaMapper extends BaseMapper<Drama> {
}
```

Create the same pattern for: `EpisodeMapper`, `CharacterMapper`, `SceneMapper`, `StoryboardMapper`, `StoryboardCharacterMapper`, `PropMapper`, `EpisodeCharacterMapper`, `EpisodeSceneMapper`, `AiServiceConfigMapper`, `AiServiceProviderMapper`, `AiVoiceMapper`, `AgentConfigMapper`, `ImageGenerationMapper`, `VideoGenerationMapper`, `VideoMergeMapper`, `AssetMapper`, `SeedanceAssetGroupMapper`, `SeedanceAssetMapper`.

- [ ] **Step 2: Create MyBatisPlusConfig**

```java
package com.huobao.drama.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@MapperScan("com.huobao.drama.mapper")
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add server/src/main/java/com/huobao/drama/mapper/ server/src/main/java/com/huobao/drama/config/MyBatisPlusConfig.java
git commit -m "feat: add MyBatis-Plus mapper interfaces and config"
```

---

### Task 6: WebConfig + Health Endpoint + Static File Serving

**Files:**
- Create: `server/src/main/java/com/huobao/drama/config/WebConfig.java`
- Create: `server/src/main/java/com/huobao/drama/controller/HealthController.java`
- Create: `server/src/main/java/com/huobao/drama/dto/ApiResponse.java`

- [ ] **Step 1: Create ApiResponse utility**

```java
package com.huobao.drama.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
```

- [ ] **Step 2: Create WebConfig**

```java
package com.huobao.drama.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${storage.local-path:../data/storage}")
    private String storagePath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3013", "http://localhost:3012")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Path.of(storagePath).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/static/**")
                .addResourceLocations(absolutePath);
    }
}
```

- [ ] **Step 3: Create HealthController**

```java
package com.huobao.drama.controller;

import com.huobao.drama.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "status", "ok",
                "timestamp", Instant.now().toString()
        ));
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/huobao/drama/config/WebConfig.java server/src/main/java/com/huobao/drama/controller/HealthController.java server/src/main/java/com/huobao/drama/dto/ApiResponse.java
git commit -m "feat: add WebConfig, health endpoint, and API response utility"
```

---

### Task 7: SQLite → MySQL Data Migration Script

**Files:**
- Create: `server/src/main/java/com/huobao/drama/migration/SqliteToMysqlMigration.java`

This is a standalone Java main class that reads from the existing SQLite database and writes to MySQL. It handles type conversions (dates, booleans) and respects foreign key ordering.

- [ ] **Step 1: Add SQLite JDBC dependency to pom.xml**

Add to `<dependencies>` section in `server/pom.xml`:

```xml
<!-- SQLite JDBC (migration only) -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.46.1.0</version>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 2: Create migration script**

```java
package com.huobao.drama.migration;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * One-time SQLite → MySQL data migration.
 * Run: mvn exec:java -Dexec.mainClass="com.huobao.drama.migration.SqliteToMysqlMigration"
 *      -Dexec.args="path/to/huobao_drama.db jdbc:mysql://localhost:3306/huobao_drama root password"
 */
public class SqliteToMysqlMigration {

    // Tables in dependency order (parents before children)
    private static final List<String> TABLES = List.of(
            "dramas",
            "episodes",
            "characters",
            "scenes",
            "props",
            "episode_characters",
            "episode_scenes",
            "storyboards",
            "storyboard_characters",
            "ai_service_configs",
            "ai_service_providers",
            "ai_voices",
            "agent_configs",
            "image_generations",
            "video_generations",
            "video_merges",
            "assets"
    );

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    );

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: <sqlite-path> <mysql-url> <mysql-user> <mysql-password>");
            System.out.println("Example: ../data/huobao_drama.db jdbc:mysql://localhost:3306/huobao_drama root root");
            return;
        }

        String sqlitePath = args[0];
        String mysqlUrl = args[1];
        String mysqlUser = args[2];
        String mysqlPass = args[3];

        try (Connection sqlite = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
             Connection mysql = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass)) {

            mysql.setAutoCommit(false);

            for (String table : TABLES) {
                migrateTable(sqlite, mysql, table);
            }

            mysql.commit();
            System.out.println("Migration completed successfully!");

            // Reset AUTO_INCREMENT for all tables
            for (String table : TABLES) {
                resetAutoIncrement(mysql, table);
            }
        }
    }

    private static void migrateTable(Connection sqlite, Connection mysql, String table) throws SQLException {
        System.out.printf("Migrating table: %s ... ", table);

        try (Statement stmt = sqlite.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // Build INSERT statement
            StringBuilder cols = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            for (int i = 1; i <= colCount; i++) {
                if (i > 1) { cols.append(", "); placeholders.append(", "); }
                cols.append("`").append(meta.getColumnName(i)).append("`");
                placeholders.append("?");
            }

            String insertSql = String.format("INSERT INTO `%s` (%s) VALUES (%s)", table, cols, placeholders);

            int count = 0;
            try (PreparedStatement ps = mysql.prepareStatement(insertSql)) {
                while (rs.next()) {
                    for (int i = 1; i <= colCount; i++) {
                        Object value = rs.getObject(i);
                        if (value instanceof String strVal && isDateColumn(meta.getColumnName(i))) {
                            ps.setObject(i, parseDateTime(strVal));
                        } else {
                            ps.setObject(i, value);
                        }
                    }
                    ps.addBatch();
                    count++;
                    if (count % 500 == 0) ps.executeBatch();
                }
                if (count % 500 != 0) ps.executeBatch();
            }

            System.out.printf("%d rows%n", count);
        }
    }

    private static boolean isDateColumn(String name) {
        return name.endsWith("_at") || name.equals("created_at") || name.equals("updated_at")
                || name.equals("deleted_at") || name.equals("completed_at");
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) return null;
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDateTime.parse(value, fmt);
            } catch (DateTimeParseException ignored) {}
        }
        // Fallback: try as-is
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            System.err.printf("  WARNING: Could not parse date '%s', storing as null%n", value);
            return null;
        }
    }

    private static void resetAutoIncrement(Connection mysql, String table) throws SQLException {
        try (Statement stmt = mysql.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT MAX(id) FROM `" + table + "`");
            if (rs.next()) {
                long maxId = rs.getLong(1);
                if (maxId > 0) {
                    stmt.execute("ALTER TABLE `" + table + "` AUTO_INCREMENT = " + (maxId + 1));
                }
            }
        } catch (SQLException e) {
            // storyboard_characters has composite PK, no auto_increment — skip
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add server/pom.xml server/src/main/java/com/huobao/drama/migration/
git commit -m "feat: add SQLite to MySQL data migration script"
```

---

### Task 8: Boot Verification — Start App and Validate Schema

**Files:** (no new files)

- [ ] **Step 1: Create MySQL database**

Run:
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS huobao_drama CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```
Expected: Query OK

- [ ] **Step 2: Start the application**

Run: `cd server && mvn spring-boot:run`
Expected: Application starts on port 5679, Flyway runs V1__init_schema.sql, logs show "Started HuobaoDramaApplication"

- [ ] **Step 3: Test health endpoint**

Run: `curl http://localhost:5679/api/v1/health`
Expected: `{"success":true,"data":{"status":"ok","timestamp":"..."},"message":null}`

- [ ] **Step 4: Verify all tables created**

Run:
```bash
mysql -u root -p huobao_drama -e "SHOW TABLES;"
```
Expected: 19 tables listed (17 existing + 2 seedance tables)

- [ ] **Step 5: Run data migration (if SQLite data exists)**

Run:
```bash
cd server && mvn exec:java -Dexec.mainClass="com.huobao.drama.migration.SqliteToMysqlMigration" -Dexec.args="../data/huobao_drama.db jdbc:mysql://localhost:3306/huobao_drama root root"
```
Expected: Each table prints row count, ends with "Migration completed successfully!"

- [ ] **Step 6: Verify migrated data**

Run:
```bash
mysql -u root -p huobao_drama -e "SELECT COUNT(*) as cnt FROM dramas; SELECT COUNT(*) as cnt FROM episodes; SELECT COUNT(*) as cnt FROM characters;"
```
Expected: Row counts match the original SQLite database

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: verify Spring Boot app boots with MySQL schema and data migration"
```

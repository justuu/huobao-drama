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

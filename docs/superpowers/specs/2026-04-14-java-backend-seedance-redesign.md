# Huobao Drama — Java 后端重写 + Seedance 2.0 流程重构

日期: 2026-04-14

## 1. 概述

两个核心变更：
1. 后端从 TypeScript/Hono 重写为 Java/Spring Boot 3，数据库从 SQLite 迁移到 MySQL 8
2. 视频生成流程重构：集成 Seedance 2.0，去掉分镜图生成环节，改为角色图+场景图+道具图直接生成视频

## 2. 技术栈

| 层 | 旧 | 新 |
|---|---|---|
| 后端框架 | Hono (Node.js) | Spring Boot 3.3+ |
| AI 框架 | Mastra | Spring AI |
| ORM | Drizzle + better-sqlite3 | MyBatis-Plus |
| 数据库 | SQLite (WAL) | MySQL 8 |
| 前端 | Vue 3 + Vite | 不变 |
| 视频生成 | 多 provider 适配器 | Seedance 2.0 (火山引擎) |

## 3. Java 项目结构

```
src/main/java/com/huobao/drama/
├── HuobaoDramaApplication.java
├── config/
│   ├── WebConfig.java              — CORS, 静态资源
│   ├── AiConfig.java               — Spring AI ChatClient Bean
│   ├── SeedanceConfig.java         — 火山引擎 AK/SK 配置
│   └── MyBatisPlusConfig.java      — 分页插件等
├── controller/
│   ├── DramaController.java        — 剧集 CRUD
│   ├── EpisodeController.java      — 集 CRUD + 流水线状态
│   ├── AgentController.java        — Agent 聊天 (SSE)
│   ├── ImageController.java        — 图片生成
│   ├── VideoController.java        — 视频生成
│   ├── ComposeController.java      — FFmpeg 合成/合并
│   ├── AssetController.java        — Seedance 素材管理
│   ├── ConfigController.java       — AI 配置管理
│   └── SkillController.java        — Skill 列表
├── entity/                          — MyBatis-Plus 实体类
├── mapper/                          — MyBatis-Plus Mapper
├── service/
│   ├── agent/
│   │   ├── AgentService.java        — Agent 编排入口
│   │   ├── SkillLoader.java         — 加载 SKILL.md
│   │   └── tools/
│   │       ├── ScriptTools.java     — 剧本改写工具
│   │       ├── ExtractTools.java    — 提取工具 (角色+场景+道具)
│   │       ├── StoryboardTools.java — 分镜工具
│   │       └── VoiceTools.java      — 音色工具
│   ├── seedance/
│   │   ├── SeedanceAssetService.java   — 素材组/素材上传
│   │   ├── SeedanceVideoService.java   — 视频生成
│   │   └── VolcSignUtil.java           — HMAC-SHA256 签名
│   ├── ImageGenerationService.java
│   ├── FfmpegComposeService.java
│   └── FfmpegMergeService.java
└── dto/                             — 请求/响应 DTO
```

## 4. 数据库设计

### 4.1 MySQL 类型映射

| SQLite | MySQL | 说明 |
|---|---|---|
| INTEGER PRIMARY KEY | BIGINT AUTO_INCREMENT | Java 用 Long |
| TEXT | VARCHAR(n) / TEXT | 短字段 VARCHAR，长内容 TEXT |
| INTEGER (boolean) | TINYINT(1) | Java 用 Boolean |
| REAL | DOUBLE | temperature 等浮点字段 |
| TEXT (datetime) | DATETIME | created_at / updated_at 改为原生时间类型 |

### 4.2 保留表（类型适配 MySQL，结构不变）

dramas, episodes, episode_characters, episode_scenes, ai_service_providers, ai_voices, agent_configs, image_generations, video_merges, assets

### 4.3 修改表

**characters** — 新增字段：
- `asset_id` VARCHAR(128) — Seedance 素材 ID
- `asset_group_id` VARCHAR(128) — 所属素材组

**scenes** — 新增字段：
- `asset_id` VARCHAR(128) — Seedance 素材 ID
- `asset_group_id` VARCHAR(128) — 所属素材组

**props** — 新增字段：
- `asset_id` VARCHAR(128) — Seedance 素材 ID
- `asset_group_id` VARCHAR(128) — 所属素材组

**storyboards** — 改造：
- 新增 `prompt_text` TEXT — 完整分镜文案（含 `<role>` `<location>` `<prop>` 标签）
- 新增 `duration_ms` INT — 毫秒精度时长
- 新增 `group_index` INT — 同一次生成中的分镜组序号
- 新增 `group_id` VARCHAR(64) — 分镜组标识（同组一起生成视频）
- 移除 `image_prompt` — 不再生成分镜图
- 移除 `composed_image`, `first_frame_image`, `last_frame_image`

**video_generations** — 改造：
- 新增 `seedance_task_id` VARCHAR(128) — Seedance 任务 ID
- 新增 `asset_refs` TEXT (JSON) — 引用的素材 ID 列表
- 新增 `storyboard_group_id` VARCHAR(64) — 对应分镜组
- 新增 `prompt_text` TEXT — 实际发送给 Seedance 的完整提示词（替换标签后）
- 移除 `image_gen_id`, `reference_mode`, `first_frame_url`, `last_frame_url`

**ai_service_configs** — 新增字段：
- `access_key` VARCHAR(256) — 火山引擎 AK（素材 API 用）
- `secret_key` VARCHAR(256) — 火山引擎 SK（素材 API 用）

### 4.4 新增表

**seedance_asset_groups**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | 自增主键 |
| drama_id | BIGINT NOT NULL | 关联 drama（一个小说一个素材组） |
| group_name | VARCHAR(128) | 素材组名称 |
| group_type | VARCHAR(32) | 固定 "AIGC" |
| seedance_group_id | VARCHAR(128) | Seedance 返回的组 ID |
| status | VARCHAR(32) | pending / ready / failed |
| created_at | DATETIME | |
| updated_at | DATETIME | |

**seedance_assets**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | 自增主键 |
| group_id | BIGINT NOT NULL | 关联 seedance_asset_groups |
| drama_id | BIGINT NOT NULL | 关联 drama |
| ref_type | VARCHAR(32) NOT NULL | character / scene / prop |
| ref_id | BIGINT NOT NULL | 关联 characters/scenes/props 表的 id |
| name | VARCHAR(128) NOT NULL | 素材名称（组内唯一） |
| asset_id | VARCHAR(128) | Seedance 返回的素材 ID (asset-xxx) |
| image_url | VARCHAR(512) | 原始图片 URL |
| status | VARCHAR(32) | pending / uploading / ready / failed |
| created_at | DATETIME | |
| updated_at | DATETIME | |

约束: `UNIQUE(group_id, name)` — 同一素材组内名称不能重复

## 5. Agent 架构 (Spring AI)

### 5.1 Skill 加载

`skills/` 目录和 SKILL.md 文件原样保留。Java 端 `SkillLoader` 读取 markdown 文件，去掉 frontmatter，拼接到 Agent 的 system prompt 中：

```java
String basePrompt = agentConfig.getSystemPrompt();
String skillContent = skillLoader.load("storyboard_breaker");
chatClient.prompt()
    .system(basePrompt + "\n\n" + skillContent)
    .user(userMessage)
    .functions(toolSet)
    .call();
```

### 5.2 Agent 类型 → Tool 映射

| Agent Type | Skill | Tools (FunctionCallback) |
|---|---|---|
| script_rewriter | script_rewriter/SKILL.md | read_episode_script, rewrite_to_screenplay, save_script |
| extractor | extractor/SKILL.md (扩展) | read_script, read_existing_characters, read_existing_scenes, **read_existing_props**, save_dedup_characters, save_dedup_scenes, **save_dedup_props** |
| storyboard_breaker | 新 sd2-pe SKILL.md | read_storyboard_context, **read_asset_mapping**, save_storyboards, update_storyboard |
| voice_assigner | — | list_voices, get_characters, assign_voice |

**read_asset_mapping** — 新增工具，返回当前 drama 下所有角色/场景/道具的 `{name → asset_id}` 映射表，供 Agent 在分镜文案中关联素材。

**extractor SKILL.md 扩展** — 在现有角色/场景提取规范基础上，增加道具提取规范：
- 提取剧中反复出现的关键道具（如武器、文件、信物等）
- 按名字精确匹配去重
- 每个道具需要: name, type, description, prompt（图片生成提示词）

### 5.3 SSE 流式响应

AgentController 使用 Spring 的 `SseEmitter` 或 `Flux<ServerSentEvent>` 实现流式返回，和前端现有的 SSE async generator 对接。

## 6. Seedance 2.0 集成

### 6.1 两套 API 两套认证

**素材管理 API：**
- Host: `ark.cn-beijing.volcengineapi.com`
- 认证: HMAC-SHA256 签名 (AK/SK)
- 接口:
  - `CreateAssetGroup` — 每个 drama 创建一个素材组
  - `CreateAsset` — 上传角色图/场景图/道具图到素材组，轮询状态直到 ready

**视频生成 API：**
- Host: `ark.cn-beijing.volces.com/api/v3`
- 认证: Bearer Token (ARK_API_KEY)
- 接口:
  - `POST /contents/generations/tasks` — 创建视频生成任务
  - `GET /contents/generations/tasks/{id}` — 轮询任务状态

### 6.2 视频生成约束

单次视频生成请求的硬性限制：

| 约束 | 限制 |
|---|---|
| 视频时长 | 5-15 秒 |
| 文本提示词 | 800 字以内 |
| 参考图片 (reference_image) | 0-9 张 |
| 参考视频 (reference_video) | 0-3 个 |
| 参考音频 (reference_audio) | 0-3 个 |

这些约束影响分镜组的拆分逻辑 — storyboard_breaker Agent 在拆分镜时需要同时考虑：
1. 总时长不超过 15 秒
2. 文案总字数不超过 800 字
3. 引用的去重素材图片（角色+场景+道具）不超过 9 张

如果任一条件超出，需要拆分成多个分镜组。

### 6.3 素材名唯一性

同一素材组内素材名不能重复。上传前先查询 `seedance_assets` 表，如果已存在同名素材则跳过上传，直接复用已有的 asset_id。

## 7. 视频生成请求构建

### 7.1 分镜文案 → Seedance API 请求的转换流程

```
输入: 一组分镜 (group_id 相同)
  分镜1 (6000ms): ...<role>宋正德--基础形象</role>...<role>沈秋水--基础形象</role>...
  分镜2 (5000ms): ...<role>张大志--基础形象</role>...<prop>水果刀</prop>...
  场景: <location>宋宅大厅</location>

处理步骤:
  1. 解析所有 <role> <location> <prop> 标签 → 去重
  2. 查 characters/scenes/props 表拿 asset_id
  3. 按出现顺序分配 @图N 编号
  4. 替换文案中的标签为 @图N（名称）格式
  5. 校验约束: 图片≤9, 文案≤800字, 时长5-15秒
  6. 构建 content 数组发送给 Seedance
```

### 7.2 构建后的请求示例

```json
{
  "model": "doubao-seedance-2-0-260128",
  "content": [
    {
      "type": "text",
      "text": "画面风格和类型: 华语真人写实电影。\n生成一个由以下2个分镜组成的视频。\n本片段场景设定在: @图1（宋宅大厅）。\n分镜1<duration-ms>6000</duration-ms>: ...@图2（宋正德）...@图3（沈秋水）...\n分镜2<duration-ms>5000</duration-ms>: ...@图4（张大志）...@图5（水果刀）..."
    },
    { "type": "image_url", "image_url": { "url": "asset://asset-scene-001" }, "role": "reference_image" },
    { "type": "image_url", "image_url": { "url": "asset://asset-char-songzhengde" }, "role": "reference_image" },
    { "type": "image_url", "image_url": { "url": "asset://asset-char-shenqiushui" }, "role": "reference_image" },
    { "type": "image_url", "image_url": { "url": "asset://asset-char-zhangdazhi" }, "role": "reference_image" },
    { "type": "image_url", "image_url": { "url": "asset://asset-prop-knife" }, "role": "reference_image" }
  ],
  "generate_audio": true,
  "ratio": "16:9",
  "duration": 11,
  "watermark": false
}
```

## 8. 生成流水线

### 8.1 最终流水线步骤

| 步骤 | 名称 | 级别 | 状态 |
|---|---|---|---|
| 1 | 导入剧本 → AI 改写 | episode | 保留 |
| 2 | AI 提取角色 + 场景 + 道具 | episode | 扩展 |
| 3 | 生成角色图 + 场景图 + 道具图 | drama | 新增 |
| 4 | 上传素材 → 获得 Asset ID | drama | 新增 |
| 5 | 音色分配 | episode | 保留 |
| 6 | AI 分镜拆解 (含 asset ID 映射) | episode | 改造 |
| 7 | Seedance 2.0 视频生成 (按分镜组) | episode | 替换 |
| 8 | FFmpeg 合成 + 合并 | episode | 保留 |

步骤 3/4 属于 drama 级别 — 一个小说的所有集共享同一套素材。首集执行时创建素材组并上传，后续集复用已有素材，仅上传新增的角色/场景/道具。

### 8.2 分镜组拆分规则

storyboard_breaker Agent 在拆分镜时，需要将分镜组合为"分镜组"，每组满足：
- 总时长: 5-15 秒
- 文案总字数: ≤ 800 字
- 去重素材图片数: ≤ 9 张（角色+场景+道具）
- 每个分镜的 duration 与对白量匹配，避免语速不自然

同一组的分镜共享 `group_id`，一次性发送给 Seedance 生成一个视频。

## 9. 前端改造

### 9.1 分镜编辑器

分镜文案中的标签渲染为可编辑卡片：
- `<role>角色名--形象</role>` → 紫色角色卡片（带缩略图）
- `<location>场景名</location>` → 蓝色场景卡片（带缩略图）
- `<prop>道具名</prop>` → 橙色道具卡片（带缩略图）

底部汇总引用的素材列表 + "生成视频"按钮。

### 9.2 API 端点变更

所有现有 `/api/v1/*` 路径保持不变，Java 端重新实现。新增端点：

| 端点 | 说明 |
|---|---|
| POST /api/v1/seedance/asset-groups | 创建素材组 |
| POST /api/v1/seedance/assets | 上传素材 |
| GET /api/v1/seedance/assets?dramaId=x | 查询素材列表和状态 |
| POST /api/v1/videos/seedance | Seedance 视频生成（按分镜组） |

### 9.3 流水线状态

前端 pipeline-status 接口返回的步骤调整为 8 步（见 8.1），移除旧的 `generate_images` 步骤。

## 10. 数据迁移

### 10.1 迁移策略

1. Flyway migration `V1__init.sql` — 生成完整 MySQL DDL（含新增字段和新表）
2. Spring Boot 启动时自动执行 DDL
3. 一次性迁移脚本 — 读 SQLite → 类型转换 → 写 MySQL
4. 数据校验 — 对比行数 + 抽样校验

### 10.2 迁移脚本处理要点

- 日期字段: SQLite TEXT ("2024-01-01T12:00:00Z") → MySQL DATETIME
- 布尔字段: SQLite INTEGER (0/1) → MySQL TINYINT(1)
- JSON 字段: 保持 TEXT 存储，内容不变
- 自增 ID: 迁移后重置 AUTO_INCREMENT 为 MAX(id) + 1
- 外键关系: 按依赖顺序导入 (dramas → episodes → characters → scenes → props → ...)
- 新增字段 (asset_id 等): 迁移后为 NULL，后续使用时填充

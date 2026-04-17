# CLAUDE.md

## 项目概述

火宝短剧 (Huobao Drama) — AI 驱动的短剧视频制作平台。采用前后端分离架构，基于 Spring Boot 3 + Nuxt 3 构建。

## 技术栈

### 后端 (server/)
- **框架**: Spring Boot 3.3.6 + Java 17
- **数据库**: MySQL 8.0 + MyBatis-Plus 3.5.9
- **AI 集成**: Spring AI 1.1.2 (OpenAI API 兼容)
- **数据库迁移**: Flyway
- **JSON 处理**: Jackson
- **构建工具**: Maven

### 前端 (frontend/)
- **框架**: Nuxt 3.17.5 + Vue 3.5.13
- **路由**: Vue Router 4.5.1
- **UI 组件**: Lucide Vue Next (图标), Vue Sonner (通知)
- **构建工具**: Vite
- **渲染模式**: SPA (ssr: false)

## 项目结构

```
huobao-drama/
├── server/                          # Spring Boot 后端
│   ├── src/main/java/com/huobao/drama/
│   │   ├── config/                  # 配置类 (AI, MyBatis, CORS)
│   │   ├── controller/              # REST API 控制器
│   │   ├── dto/                     # 数据传输对象
│   │   ├── entity/                  # 数据库实体类
│   │   ├── mapper/                  # MyBatis Mapper 接口
│   │   ├── service/                 # 业务逻辑层
│   │   │   ├── agent/               # AI Agent 服务
│   │   │   │   └── tools/           # Agent 工具类
│   │   │   └── seedance/            # Seedance 2.0 集成
│   │   ├── exception/               # 异常处理
│   │   └── migration/               # 数据迁移工具
│   ├── src/main/resources/
│   │   ├── application.yml          # 应用配置
│   │   ├── db/migration/            # Flyway 迁移脚本
│   │   └── skills/                  # AI Agent 技能定义
│   │       ├── extractor/           # 角色场景提取
│   │       ├── script_rewriter/     # 剧本改写
│   │       ├── storyboard_breaker/  # 分镜拆解
│   │       ├── voice_assigner/      # 音色分配
│   │       └── grid_prompt_generator/ # 网格提示词生成
│   └── pom.xml
├── frontend/                        # Nuxt 3 前端
│   ├── app/
│   │   ├── pages/                   # 页面路由
│   │   │   ├── index.vue            # 项目列表
│   │   │   ├── settings.vue         # 系统设置
│   │   │   └── drama/[id]/          # 剧集详情
│   │   ├── components/              # Vue 组件
│   │   ├── composables/             # 组合式函数
│   │   ├── layouts/                 # 布局模板
│   │   └── assets/                  # 静态资源
│   ├── nuxt.config.ts               # Nuxt 配置
│   └── package.json
├── configs/                         # 配置文件目录
├── docs/                            # 文档目录
└── design/                          # 设计文档
```

## 核心功能模块

### 1. 剧本管理
- 剧集创建与编辑
- 剧本内容管理
- AI 剧本改写 (script_rewriter agent)

### 2. 角色与场景提取
- 从剧本自动提取角色、场景、道具
- 生成角色外貌描述和图片提示词
- 场景环境渲染提示词生成
- 关键道具识别与描述
- **Agent**: extractor (ExtractTools)

### 3. 分镜拆解
- 将剧本拆解为分镜序列
- 生成每个分镜的视觉描述
- 镜头类型、角度、运动设定
- 对白、动作、音效配置
- **Agent**: storyboard_breaker (StoryboardTools)

### 4. 音色分配
- 为角色分配 AI 语音音色
- 支持多种 TTS 提供商
- 音色预览与试听
- **Agent**: voice_assigner (VoiceTools)

### 5. 图片生成
- 角色形象生成 (四视图/非对称水平排版)
- 场景背景渲染
- 道具物品生成
- 支持多种 AI 图片生成服务

### 6. 视频合成
- 分镜视频生成
- 音频合成与配音
- 视频片段合并
- 最终成片导出

### 7. Seedance 2.0 集成
- 角色一致性管理 (Asset)
- 场景资产复用
- 批量生成与训练

## 数据库设计

### 核心表
- `dramas` — 剧集项目
- `episodes` — 剧集分集
- `characters` — 角色信息
- `scenes` — 场景信息
- `props` — 道具信息
- `storyboards` — 分镜信息
- `ai_service_configs` — AI 服务配置
- `agent_configs` — Agent 配置
- `ai_voices` — AI 音色库
- `seedance_assets` — Seedance 资产

### 关联表
- `episode_characters` — 剧集-角色关联
- `episode_scenes` — 剧集-场景关联
- `storyboard_characters` — 分镜-角色关联

## AI Agent 架构

### Agent 类型
1. **script_rewriter** — 剧本改写助手
2. **extractor** — 角色场景提取助手
3. **storyboard_breaker** — 分镜拆解助手
4. **voice_assigner** — 音色分配助手

### Agent 工具 (Tools)
每个 Agent 通过 Spring AI 的 `@Tool` 注解暴露工具方法：

- **ScriptTools**: getScript, updateScript
- **ExtractTools**: readScriptForExtraction, readExistingCharacters, readExistingScenes, readExistingProps, saveDedupCharacters, saveDedupScenes, saveDedupProps
- **StoryboardTools**: getStoryboards, createStoryboard, updateStoryboardTool
- **VoiceTools**: getVoices, assignVoice

### Skill 定义
每个 Agent 的行为由 `SKILL.md` 文件定义，包含：
- 任务描述与规范
- 工具使用步骤
- 输出格式要求
- 质量标准

## API 端点

### 剧集管理
- `GET /api/dramas` — 获取剧集列表
- `POST /api/dramas` — 创建剧集
- `GET /api/dramas/{id}` — 获取剧集详情
- `PUT /api/dramas/{id}` — 更新剧集
- `DELETE /api/dramas/{id}` — 删除剧集

### 分集管理
- `GET /api/episodes?dramaId={id}` — 获取分集列表
- `POST /api/episodes` — 创建分集
- `PUT /api/episodes/{id}` — 更新分集

### 角色/场景/道具
- `GET /api/characters?dramaId={id}`
- `GET /api/scenes?dramaId={id}`
- `GET /api/props?dramaId={id}`

### AI Agent
- `POST /api/agent/chat` — Agent 对话 (SSE 流式响应)
- `GET /api/agent/configs` — 获取 Agent 配置
- `POST /api/agent/configs` — 创建/更新 Agent 配置

### AI 服务配置
- `GET /api/ai/configs` — 获取 AI 服务配置
- `POST /api/ai/configs` — 保存 AI 服务配置
- `GET /api/ai/providers` — 获取支持的 AI 提供商

### 音色管理
- `GET /api/ai/voices` — 获取音色列表

## 开发命令

### 后端 (server/)
```bash
# 开发模式 (端口 5679)
mvn spring-boot:run

# 构建
mvn clean package

# 类型检查
mvn compile
```

### 前端 (frontend/)
```bash
# 开发模式 (端口 3013, 代理 /api 到 5679)
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

## 配置说明

### application.yml
- `server.port`: 5679
- `storage.local-path`: ../data/storage
- `storage.base-url`: http://localhost:5679/static
- `skills.path`: ../skills (已废弃，现使用 classpath:skills/)

### nuxt.config.ts
- `srcDir`: app/
- `ssr`: false (SPA 模式)
- Vite 代理: /api → http://localhost:5679

### AI 服务配置
通过数据库 `ai_service_configs` 表配置：
- `service_type`: text/image/video/audio
- `provider`: openai/volcengine/minimax 等
- `api_key`, `base_url`, `model_name`

## 重要注意事项

### JSON 格式要求
AI Agent 调用工具时，JSON 参数必须严格遵循标准格式：
- 字段名必须用双引号: `{"name":"value"}` ✓
- 不能使用 Java toString 格式: `{name=value}` ✗
- 所有字符串值必须用双引号包裹

### 角色提取规则
- 只提取有对话的主角/配角，不提取龙套
- 每个角色必须包含完整的 `prompt` 字段
- 角色提示词包含外貌、服装、姿态、技术要求

### 场景提取规则
- 相同 location 的场景会更新 time 和 prompt
- 场景提示词为纯背景，不含人物

### 道具提取规则
- 只提取剧情关键道具
- 不提取常见背景物品 (桌椅、杯子等)

## 开发规范

### 代码风格
- Java: 遵循 Spring Boot 最佳实践
- TypeScript/Vue: 使用 Composition API
- 命名: 数据库字段 snake_case, Java 属性 camelCase

### Git 工作流
- 主分支: master
- 提交信息: 使用 Conventional Commits 格式

### 数据库迁移
- 使用 Flyway 管理 schema 变更
- 迁移文件位于 `src/main/resources/db/migration/`
- 命名格式: `V{version}__{description}.sql`

## 已知问题

### 中文 AI 模型兼容性
使用火山引擎等中文 AI 模型时，可能出现工具调用格式不符合 OpenAI 标准的情况。已通过在 `@Tool` description 中添加明确的 JSON 格式示例来缓解。

### Seedance 2.0 集成
部分 Seedance API 功能仍在开发中，Asset 管理和批量生成功能可能不稳定。

## 相关文档

- [README.md](./README.md) — 项目介绍与快速开始
- [docs/](./docs/) — 详细技术文档
- [design/](./design/) — 设计文档与原型

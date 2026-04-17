# 角色、场景、道具提示词增强实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为角色、场景、道具实现详细的中文提示词生成功能，提升 AI 图片生成质量

**Architecture:** 在 Extractor Agent 中集成提示词生成逻辑，通过 ExtractTools 工具在创建实体时自动生成详细提示词。角色采用两步生成（appearance → prompt），场景和道具直接生成 prompt。提供 regenerate-prompt 端点支持手动重新生成。

**Tech Stack:** Spring Boot, MyBatis-Plus, Spring AI, OpenAI API

---

## Task 1: 数据库迁移 - 添加 prompt 字段

**Files:**
- Execute: SQL migration script

- [ ] **Step 1: 创建迁移 SQL 文件**

创建文件 `server/src/main/resources/db/migration/V001__add_character_prompt.sql`:

```sql
-- Add prompt field to characters table
ALTER TABLE characters ADD COLUMN prompt TEXT;
```

- [ ] **Step 2: 执行迁移**

运行命令:
```bash
cd server
# 使用 SQLite 命令行工具执行迁移
sqlite3 ../data/drama_generator.db < src/main/resources/db/migration/V001__add_character_prompt.sql
```

预期输出: 无错误信息

- [ ] **Step 3: 验证字段已添加**

运行命令:
```bash
sqlite3 ../data/drama_generator.db "PRAGMA table_info(characters);"
```

预期输出: 包含 `prompt` 字段的列信息

- [ ] **Step 4: Commit**

```bash
git add server/src/main/resources/db/migration/V001__add_character_prompt.sql
git commit -m "feat: add prompt field to characters table"
```

---

## Task 2: 更新 Character 实体类

**Files:**
- Modify: `server/src/main/java/com/huobao/drama/entity/Character.java:17`

- [ ] **Step 1: 添加 prompt 字段**

在 `Character.java` 的 `appearance` 字段后添加:

```java
private String prompt;
```

完整上下文（第 15-18 行）:
```java
    private String description;
    private String appearance;
    private String prompt;
    private String personality;
```

- [ ] **Step 2: 编译验证**

运行命令:
```bash
cd server
npm run typecheck
```

预期输出: 无编译错误

- [ ] **Step 3: Commit**

```bash
git add server/src/main/java/com/huobao/drama/entity/Character.java
git commit -m "feat: add prompt field to Character entity"
```

---

## Task 3: 更新 ExtractTools - 角色提示词生成

**Files:**
- Modify: `server/src/main/java/com/huobao/drama/service/agent/tools/ExtractTools.java:62-97`

- [ ] **Step 1: 更新 saveDedupCharacters 方法签名**

在 `saveDedupCharacters` 方法中添加 `prompt` 参数。修改第 62 行的方法签名:

```java
public String saveDedupCharacters(Long episodeId, Long dramaId, String charactersJson, String prompt) {
```

- [ ] **Step 2: 解析 prompt 参数**

在方法内部，解析 prompt JSON（在 characters 数组解析之后，第 70 行附近）:

```java
        List<Map<String, Object>> characters = (List<Map<String, Object>>) parsed.get("characters");
        Map<String, String> prompts = new HashMap<>();
        if (prompt != null && !prompt.isEmpty()) {
            try {
                Map<String, Object> promptParsed = objectMapper.readValue(prompt, Map.class);
                Map<String, Object> promptsMap = (Map<String, Object>) promptParsed.get("prompts");
                if (promptsMap != null) {
                    promptsMap.forEach((k, v) -> prompts.put(k, v.toString()));
                }
            } catch (Exception e) {
                log.warn("Failed to parse prompts: {}", e.getMessage());
            }
        }
```

- [ ] **Step 3: 保存 prompt 到角色实体**

在角色保存逻辑中（第 85-90 行附近），添加 prompt 字段设置:

```java
                c.setDescription((String) ch.get("description"));
                c.setAppearance((String) ch.get("appearance"));
                c.setPrompt(prompts.getOrDefault(name, (String) ch.get("prompt")));
                c.setPersonality((String) ch.get("personality"));
```

- [ ] **Step 4: 编译验证**

运行命令:
```bash
cd server
npm run typecheck
```

预期输出: 无编译错误

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/huobao/drama/service/agent/tools/ExtractTools.java
git commit -m "feat: update ExtractTools to handle character prompt field"
```

---

## Task 4: 更新 Extractor Agent System Prompt

**Files:**
- Modify: `server/skills/extractor/SKILL.md`

- [ ] **Step 1: 读取现有 SKILL.md**

运行命令:
```bash
cat server/skills/extractor/SKILL.md
```

- [ ] **Step 2: 在 SKILL.md 中添加提示词生成指导**

在工具说明部分之前添加以下内容:

```markdown
## 提示词生成要求

### 角色提示词生成（两步）

**步骤 1：生成 appearance（详细外貌）**

基于角色描述，生成详细的外貌特征描述（中文），包含：
- 年龄与体型：年龄段、身高体型、体态特征
- 发型：长度、颜色、造型、质感
- 面部特征：脸型、眉形、眼型、鼻型、唇形
- 肤色与妆容：肤色、妆容风格、重点妆容

要求：具体、视觉化、适合图片生成

**步骤 2：生成 prompt（完整图片提示词）**

基于 description + appearance，生成完整的图片生成提示词（中文），包含：
1. 外貌特征（基于 appearance）
2. 服装细节（面料、颜色、款式、剪裁）
3. 配饰细节
4. 姿态与气质
5. 技术要求：白底四视图，从左到右依次为：正脸特写（肩部以上，展示面部细节）、全身正面、全身左侧面、全身右侧面。人物居中，光线均匀，无阴影

输出格式：一段连贯的中文描述，适合直接用于图片生成

### 场景提示词生成

基于场景的 location 和 time，生成完整的环境渲染提示词（中文），包含：
1. 空间布局与构图
2. 光线与氛围（根据时间）
3. 色调与质感
4. 环境细节（家具、装饰、道具）
5. 技术要求：电影级渲染，景深效果，高清细节

输出格式：一段连贯的中文描述

### 道具提示词生成

**重要：只提取对剧情有重要作用的关键道具，忽略常见背景物品（如桌椅、杯子、笔等）**

基于道具的 description，生成完整的图片提示词（中文），包含：
1. 道具的材质、颜色、尺寸、造型
2. 细节特征（纹理、装饰、磨损等）
3. 技术要求：白底三视图，包含正面、左侧面、俯视图，物体居中，光线均匀，无阴影

输出格式：一段连贯的中文描述

所有描述使用中文，具体、视觉化、适合图片生成。
```

- [ ] **Step 3: 验证文件格式**

运行命令:
```bash
cat server/skills/extractor/SKILL.md | head -50
```

预期输出: 包含新添加的提示词生成要求部分

- [ ] **Step 4: Commit**

```bash
git add server/skills/extractor/SKILL.md
git commit -m "feat: add prompt generation guidelines to extractor agent"
```

---

## Task 5: 添加 CharacterController regenerate-prompt 端点

**Files:**
- Modify: `server/src/main/java/com/huobao/drama/controller/CharacterController.java:47`
- Modify: `server/src/main/java/com/huobao/drama/service/CharacterService.java`

- [ ] **Step 1: 在 CharacterService 中添加 regeneratePrompt 方法**

在 `CharacterService.java` 中添加方法:

```java
public Character regeneratePrompt(Long id) {
    Character character = characterMapper.selectById(id);
    if (character == null) {
        return null;
    }
    
    // TODO: 调用 agent 重新生成 appearance 和 prompt
    // 这里需要集成 AgentService 来调用 extractor agent
    // 暂时返回原对象，后续实现
    
    return character;
}
```

- [ ] **Step 2: 在 CharacterController 中添加端点**

在 `CharacterController.java` 的 `batchGenerateImages` 方法后添加:

```java
@PostMapping("/{id}/regenerate-prompt")
public ApiResponse<Character> regeneratePrompt(@PathVariable Long id) {
    Character character = characterService.regeneratePrompt(id);
    if (character == null) {
        return ApiResponse.notFound("Character not found");
    }
    return ApiResponse.ok(character);
}
```

- [ ] **Step 3: 编译验证**

运行命令:
```bash
cd server
npm run typecheck
```

预期输出: 无编译错误

- [ ] **Step 4: Commit**

```bash
git add server/src/main/java/com/huobao/drama/controller/CharacterController.java server/src/main/java/com/huobao/drama/service/CharacterService.java
git commit -m "feat: add regenerate-prompt endpoint for characters"
```

---

## Task 6: 添加 SceneController regenerate-prompt 端点

**Files:**
- Modify: `server/src/main/java/com/huobao/drama/controller/SceneController.java:47`
- Modify: `server/src/main/java/com/huobao/drama/service/SceneService.java`

- [ ] **Step 1: 在 SceneService 中添加 regeneratePrompt 方法**

在 `SceneService.java` 中添加方法:

```java
public Scene regeneratePrompt(Long id) {
    Scene scene = sceneMapper.selectById(id);
    if (scene == null) {
        return null;
    }
    
    // TODO: 调用 agent 重新生成 prompt
    // 暂时返回原对象，后续实现
    
    return scene;
}
```

- [ ] **Step 2: 在 SceneController 中添加端点**

在 `SceneController.java` 的 `deleteScene` 方法后添加:

```java
@PostMapping("/{id}/regenerate-prompt")
public ApiResponse<Scene> regeneratePrompt(@PathVariable Long id) {
    Scene scene = sceneService.regeneratePrompt(id);
    if (scene == null) {
        return ApiResponse.notFound("Scene not found");
    }
    return ApiResponse.ok(scene);
}
```

- [ ] **Step 3: 编译验证**

运行命令:
```bash
cd server
npm run typecheck
```

预期输出: 无编译错误

- [ ] **Step 4: Commit**

```bash
git add server/src/main/java/com/huobao/drama/controller/SceneController.java server/src/main/java/com/huobao/drama/service/SceneService.java
git commit -m "feat: add regenerate-prompt endpoint for scenes"
```

---

## Task 7: 添加 PropController regenerate-prompt 端点

**Files:**
- Modify: `server/src/main/java/com/huobao/drama/controller/PropController.java:54`
- Modify: `server/src/main/java/com/huobao/drama/service/PropService.java`

- [ ] **Step 1: 在 PropService 中添加 regeneratePrompt 方法**

在 `PropService.java` 中添加方法:

```java
public Prop regeneratePrompt(Long id) {
    Prop prop = propMapper.selectById(id);
    if (prop == null) {
        return null;
    }
    
    // TODO: 调用 agent 重新生成 prompt
    // 暂时返回原对象，后续实现
    
    return prop;
}
```

- [ ] **Step 2: 在 PropController 中添加端点**

在 `PropController.java` 的 `generateImage` 方法后添加:

```java
@PostMapping("/{id}/regenerate-prompt")
public ApiResponse<Prop> regeneratePrompt(@PathVariable Long id) {
    Prop prop = propService.regeneratePrompt(id);
    if (prop == null) {
        return ApiResponse.notFound("Prop not found");
    }
    return ApiResponse.ok(prop);
}
```

- [ ] **Step 3: 编译验证**

运行命令:
```bash
cd server
npm run typecheck
```

预期输出: 无编译错误

- [ ] **Step 4: Commit**

```bash
git add server/src/main/java/com/huobao/drama/controller/PropController.java server/src/main/java/com/huobao/drama/service/PropService.java
git commit -m "feat: add regenerate-prompt endpoint for props"
```

---

## Task 8: 更新 ImageGenerationService 使用 prompt 字段

**Files:**
- Modify: `server/src/main/java/com/huobao/drama/service/ImageGenerationService.java:63,80,97`

- [ ] **Step 1: 更新 generateForCharacter 方法**

修改第 63 行，从使用 `description` 改为使用 `prompt`:

```java
String prompt = (String) params.getOrDefault("prompt", 
    character.getPrompt() != null ? character.getPrompt() : character.getDescription());
```

- [ ] **Step 2: 更新 generateForProp 方法**

修改第 97 行，从使用 `description` 改为使用 `prompt`:

```java
String prompt = (String) params.getOrDefault("prompt", 
    prop.getPrompt() != null ? prop.getPrompt() : prop.getDescription());
```

- [ ] **Step 3: 验证 generateForScene 已使用 prompt**

确认第 80 行已经使用 `scene.getPrompt()`（无需修改）

- [ ] **Step 4: 编译验证**

运行命令:
```bash
cd server
npm run typecheck
```

预期输出: 无编译错误

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/huobao/drama/service/ImageGenerationService.java
git commit -m "feat: update ImageGenerationService to use prompt fields"
```

---

## Task 9: 集成测试 - 端到端验证

**Files:**
- Test: Manual API testing

- [ ] **Step 1: 启动后端服务**

运行命令:
```bash
cd server
npm run dev
```

预期输出: 服务在端口 5679 启动

- [ ] **Step 2: 测试角色 regenerate-prompt 端点**

运行命令:
```bash
curl -X POST http://localhost:5679/api/v1/characters/1/regenerate-prompt
```

预期输出: 返回角色对象 JSON（status 200 或 404）

- [ ] **Step 3: 测试场景 regenerate-prompt 端点**

运行命令:
```bash
curl -X POST http://localhost:5679/api/v1/scenes/1/regenerate-prompt
```

预期输出: 返回场景对象 JSON（status 200 或 404）

- [ ] **Step 4: 测试道具 regenerate-prompt 端点**

运行命令:
```bash
curl -X POST http://localhost:5679/api/v1/props/1/regenerate-prompt
```

预期输出: 返回道具对象 JSON（status 200 或 404）

- [ ] **Step 5: 验证数据库字段**

运行命令:
```bash
sqlite3 data/drama_generator.db "SELECT id, name, prompt FROM characters LIMIT 1;"
```

预期输出: 显示角色的 prompt 字段（可能为 NULL）

- [ ] **Step 6: 记录测试结果**

创建测试报告文件 `docs/superpowers/test-results/2026-04-17-prompt-enhancement.md`，记录所有端点的测试结果

- [ ] **Step 7: Commit**

```bash
git add docs/superpowers/test-results/2026-04-17-prompt-enhancement.md
git commit -m "test: add integration test results for prompt enhancement"
```

---

## 实现注意事项

1. **Agent 集成**: Tasks 5-7 中的 regeneratePrompt 方法标记为 TODO，需要后续集成 AgentService 来实际调用 extractor agent 生成提示词
2. **错误处理**: 所有 Service 方法应添加适当的异常处理和日志记录
3. **性能考虑**: 提示词生成是 LLM 调用，考虑添加异步处理或缓存机制
4. **数据验证**: 在保存提示词前验证内容不为空且符合格式要求
5. **向后兼容**: ImageGenerationService 的修改包含 fallback 逻辑，确保现有数据（prompt 为 NULL）仍能正常工作

## 后续优化

1. 实现 regeneratePrompt 方法中的 agent 调用逻辑
2. 添加批量重新生成提示词的端点
3. 添加提示词版本管理功能
4. 优化提示词生成模板，支持不同艺术风格
5. 添加提示词质量评估机制

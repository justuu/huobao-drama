# 角色、场景、道具提示词增强设计

## 概述

为角色（Character）、场景（Scene）、道具（Prop）实现详细的中文提示词生成功能，提升 AI 图片生成质量。

## 目标

1. 角色描述从简单文本（"身着旗袍，气质优雅"）升级为详细的外貌、服装、气质描述
2. 角色图片生成要求：白底四视图（正脸特写 + 全身正面 + 左侧面 + 右侧面）
3. 场景图片生成要求：完整的环境渲染图（带光影、氛围）
4. 道具图片生成要求：白底三视图（正面 + 侧面 + 俯视）
5. 所有提示词使用中文

## 数据库 Schema 调整

### Character 表添加字段

```sql
ALTER TABLE characters ADD COLUMN prompt TEXT;
```

### 字段用途说明

**Character 表：**
- `description`：extractor agent 从剧本提取的简单描述（如"豪门太太，冷艳强势"）
- `appearance`：AI 生成的详细外貌描述（年龄、体型、五官、发型、妆容等）
- `prompt`：完整的图片生成提示词（包含外貌 + 服装 + 四视图要求）

**Scene 表：**
- `prompt`：完整的环境渲染提示词（空间布局、光影、氛围、细节）

**Prop 表：**
- `description`：extractor agent 从剧本提取的简单描述
- `prompt`：完整的图片生成提示词（材质、造型 + 三视图要求）

## 架构设计

### 实现方式

提示词增强集成到 **Extractor Agent** 中，而不是在 Service 层实现。

**原因：**
1. Extractor agent 已经负责从剧本中提取角色、场景、道具
2. LLM 更适合生成详细的描述性文本
3. 避免在 Service 层重复 LLM 调用逻辑

### Extractor Agent 职责扩展

**当前职责：**
- 从剧本中提取角色、场景、道具的基础信息

**扩展职责：**
- 提取时不仅创建基础信息，还要生成详细的 `appearance`（角色）和 `prompt`（角色/场景/道具）
- 使用详细描述模板作为 agent 的 system prompt 指导

### ExtractTools 调整

**1. 创建角色（create_character）：**
- 从剧本提取 `description`（简单描述）
- Agent 同时生成 `appearance`（详细外貌）
- Agent 生成 `prompt`（四视图完整提示词）
- 三个字段一起保存到数据库

**2. 创建场景（create_scene）：**
- 从剧本提取 `location` 和 `time`
- Agent 生成 `prompt`（环境渲染提示词）

**3. 创建道具（create_prop）：**
- 从剧本提取关键道具的 `description`（只提取对剧情有重要作用的道具，忽略常见背景物品）
- Agent 生成 `prompt`（三视图提示词）

## 提示词生成模板

### 角色提示词生成（两步）

#### 步骤 1：生成 appearance（详细外貌）

**输入：** `description`（用户简单描述）

**输出：** 详细的外貌描述

**模板：**
```
基于以下角色描述，生成详细的外貌特征描述（中文）：

角色描述：{description}

请按以下结构输出：
- 年龄与体型：年龄段、身高体型、体态特征
- 发型：长度、颜色、造型、质感
- 面部特征：脸型、眉形、眼型、鼻型、唇形
- 肤色与妆容：肤色、妆容风格、重点妆容

要求：具体、视觉化、适合图片生成
```

**示例输出：**
```
25至30岁女性，身形高挑纤细，肩颈线条优雅，腰线明显，体态挺拔稳定，站姿笔直有压场感。黑色长发做成低盘发，发丝梳理整齐服帖，带自然冷黑光泽，额前保留少量细碎鬓发修饰脸型。脸型为偏窄的鹅蛋脸，下颌线清晰；眉形为略上扬的细长剑眉，眉峰利落；眼型为狭长内双丹凤眼，眼尾微挑，瞳色深，目光锐利；鼻型为直挺秀气的窄鼻梁；唇形为线条清晰的薄唇偏饱满，唇峰分明。肤色冷白，整体妆容克制高级，底妆干净，眼妆偏冷调，唇色为低饱和豆沙玫瑰色。
```

#### 步骤 2：生成 prompt（完整图片提示词）

**输入：** `description` + `appearance`

**输出：** 完整的图片生成提示词

**模板：**
```
基于以下信息，生成角色图片的完整提示词（中文）：

角色描述：{description}
外貌特征：{appearance}

请生成包含以下内容的完整提示词：
1. 外貌特征（基于上述 appearance）
2. 服装细节（面料、颜色、款式、剪裁）
3. 配饰细节
4. 姿态与气质
5. 技术要求：白底四视图，从左到右依次为：正脸特写（肩部以上，展示面部细节）、全身正面、全身左侧面、全身右侧面。人物居中，光线均匀，无阴影

输出格式：一段连贯的中文描述，适合直接用于图片生成
```

**示例输出：**
```
25至30岁女性，身形高挑纤细，肩颈线条优雅，腰线明显，体态挺拔稳定，站姿笔直有压场感。黑色长发做成低盘发，发丝梳理整齐服帖，带自然冷黑光泽，额前保留少量细碎鬓发修饰脸型。脸型为偏窄的鹅蛋脸，下颌线清晰；眉形为略上扬的细长剑眉，眉峰利落；眼型为狭长内双丹凤眼，眼尾微挑，瞳色深，目光锐利；鼻型为直挺秀气的窄鼻梁；唇形为线条清晰的薄唇偏饱满，唇峰分明。肤色冷白，整体妆容克制高级，底妆干净，眼妆偏冷调，唇色为低饱和豆沙玫瑰色。穿一件浅色修身旗袍，主色为雾霭白偏浅银灰，面料为带细腻暗纹光泽的真丝提花，贴身剪裁突出腰臀比例，立领设计，盘扣精致规整，从领口延伸到右侧胸前，短袖或贴臂袖型利落，裙长过膝至小腿中段，两侧开衩适中，边缘滚细窄银线。外搭一件黑色长款大衣，面料为高支羊毛羊绒混纺，垂坠感强，宽翻领，肩线利落，版型修长挺括，无多余装饰，增强肃杀感。脚穿白色尖头高跟鞋，鞋面为细腻哑光皮革，鞋跟中高偏细。配饰克制：一对小型珍珠耳钉，一枚简洁铂金婚戒，左手佩戴窄表带腕表。整体气质冷艳、贵气、强势，兼具豪门太太的精致感与掌权者的锋利感。白底四视图，从左到右依次为：正脸特写（肩部以上，展示面部细节）、全身正面、全身左侧面、全身右侧面。人物居中，光线均匀，无阴影。
```

### 场景提示词生成

**输入：** `location` + `time`

**输出：** 完整的环境渲染提示词

**模板：**
```
基于以下场景信息，生成完整的环境渲染提示词（中文）：

地点：{location}
时间：{time}

请生成包含以下内容的完整提示词：
1. 空间布局与构图
2. 光线与氛围（根据时间）
3. 色调与质感
4. 环境细节（家具、装饰、道具）
5. 技术要求：电影级渲染，景深效果，高清细节

输出格式：一段连贯的中文描述
```

### 道具提示词生成

**输入：** `description`

**输出：** 完整的三视图提示词

**模板：**
```
基于以下道具描述，生成完整的图片提示词（中文）：

道具描述：{description}

请生成包含以下内容的完整提示词：
1. 道具的材质、颜色、尺寸、造型
2. 细节特征（纹理、装饰、磨损等）
3. 技术要求：白底三视图，包含正面、左侧面、俯视图，物体居中，光线均匀，无阴影

输出格式：一段连贯的中文描述
```

## API 接口设计

### 新增接口

**1. `POST /api/v1/characters/{id}/regenerate-prompt`**
- 功能：重新生成角色的 appearance 和 prompt
- 请求体：空（基于数据库中现有的 description）
- 响应：更新后的 Character 对象
- 实现：调用 extractor agent，让 agent 基于现有数据重新生成

**2. `POST /api/v1/scenes/{id}/regenerate-prompt`**
- 功能：重新生成场景的 prompt
- 请求体：空（基于数据库中现有的 location 和 time）
- 响应：更新后的 Scene 对象
- 实现：调用 extractor agent

**3. `POST /api/v1/props/{id}/regenerate-prompt`**
- 功能：重新生成道具的 prompt
- 请求体：空（基于数据库中现有的 description）
- 响应：更新后的 Prop 对象
- 实现：调用 extractor agent

### 现有流程调整

**ExtractTools 工具调整：**
- `create_character`：创建时自动生成 appearance 和 prompt
- `create_scene`：创建时自动生成 prompt
- `create_prop`：创建时自动生成 prompt

**ImageGenerationService 调整：**
- 图片生成时优先使用 prompt 字段
  - 角色：使用 `character.prompt`
  - 场景：使用 `scene.prompt`
  - 道具：使用 `prop.prompt`

## 实现细节

### Extractor Agent System Prompt 更新

在 extractor agent 的 system prompt 中添加提示词生成指导：

```
你是一个剧本分析专家，负责从剧本中提取角色、场景、道具信息。

对于角色：
1. 提取简单描述（description）
2. 生成详细外貌描述（appearance），包含：年龄体型、发型、面部特征、肤色妆容
3. 生成完整图片提示词（prompt），包含：外貌、服装、配饰、姿态、气质，以及技术要求（白底四视图：正脸特写 + 全身正面 + 左侧面 + 右侧面）

对于场景：
1. 提取地点（location）和时间（time）
2. 生成完整环境渲染提示词（prompt），包含：空间布局、光线氛围、色调质感、环境细节，以及技术要求（电影级渲染、景深效果）

对于道具：
1. 只提取关键道具（对剧情有重要作用的道具，忽略常见背景物品如桌椅、杯子等）
2. 提取简单描述（description）
3. 生成完整图片提示词（prompt），包含：材质、颜色、尺寸、造型、细节特征，以及技术要求（白底三视图：正面 + 侧面 + 俯视）

所有描述使用中文，具体、视觉化、适合图片生成。
```

### ExtractTools 工具参数调整

**create_character 工具：**
```java
// 添加 appearance 和 prompt 参数
@JsonProperty("appearance") String appearance,
@JsonProperty("prompt") String prompt
```

**create_scene 工具：**
```java
// prompt 参数已存在，确保 agent 填充
@JsonProperty("prompt") String prompt
```

**create_prop 工具：**
```java
// prompt 参数已存在，确保 agent 填充
@JsonProperty("prompt") String prompt
```

### 新增 regenerate 工具

在 ExtractTools 中添加三个新工具：

**1. regenerate_character_prompt：**
- 输入：character_id
- 读取数据库中的 description
- 调用 agent 生成新的 appearance 和 prompt
- 更新数据库

**2. regenerate_scene_prompt：**
- 输入：scene_id
- 读取数据库中的 location 和 time
- 调用 agent 生成新的 prompt
- 更新数据库

**3. regenerate_prop_prompt：**
- 输入：prop_id
- 读取数据库中的 description
- 调用 agent 生成新的 prompt
- 更新数据库

### Controller 层实现

在 CharacterController、SceneController、PropController 中添加 regenerate-prompt 端点，调用对应的 regenerate 工具。

## 错误处理

1. **Agent 调用失败：**
   - 记录错误日志
   - 返回 500 错误，提示"提示词生成失败"

2. **生成的提示词为空：**
   - 记录警告日志
   - 使用原始 description 作为 fallback

3. **数据库更新失败：**
   - 回滚事务
   - 返回 500 错误

## 测试计划

1. **单元测试：**
   - ExtractTools 的 create 和 regenerate 方法
   - 验证生成的 prompt 包含必要的技术要求（四视图、三视图等）

2. **集成测试：**
   - 创建角色/场景/道具，验证 prompt 自动生成
   - 调用 regenerate-prompt 接口，验证 prompt 更新

3. **端到端测试：**
   - 从剧本提取到图片生成的完整流程
   - 验证生成的图片符合视图要求

## 部署注意事项

1. **数据库迁移：**
   - 执行 `ALTER TABLE characters ADD COLUMN prompt TEXT;`
   - 对现有角色数据，可选择批量调用 regenerate-prompt 接口

2. **Agent 配置：**
   - 更新 extractor agent 的 system prompt
   - 确保 agent 使用的 LLM 模型支持中文生成

3. **性能考虑：**
   - 提示词生成是异步操作，不阻塞创建流程
   - 考虑添加生成状态字段（pending/completed/failed）

## 未来扩展

1. **多语言支持：**
   - 支持英文、日文等其他语言的提示词生成

2. **风格模板：**
   - 支持不同的艺术风格（写实、动漫、水彩等）

3. **批量生成：**
   - 支持批量重新生成多个实体的提示词

4. **提示词版本管理：**
   - 保存提示词的历史版本，支持回滚

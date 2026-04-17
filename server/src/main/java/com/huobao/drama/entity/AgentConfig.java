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

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

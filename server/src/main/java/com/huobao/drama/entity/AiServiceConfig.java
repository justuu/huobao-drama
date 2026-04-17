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

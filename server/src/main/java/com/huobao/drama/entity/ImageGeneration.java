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

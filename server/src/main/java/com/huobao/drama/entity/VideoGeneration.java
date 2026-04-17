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

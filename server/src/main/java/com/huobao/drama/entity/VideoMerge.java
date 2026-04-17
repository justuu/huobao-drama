package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_merges")
public class VideoMerge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long episodeId;
    private Long dramaId;
    private String title;
    private String provider;
    private String model;
    private String status;
    private String scenes;
    private String mergedUrl;
    private Integer duration;
    private String taskId;
    private String errorMsg;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}

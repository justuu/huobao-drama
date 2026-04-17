package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("scenes")
public class Scene {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dramaId;
    private Long episodeId;
    private String location;
    private String time;
    private String prompt;
    private Integer storyboardCount;
    private String imageUrl;
    private String status;
    private String localPath;
    private String assetId;
    private String assetGroupId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}

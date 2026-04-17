package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("assets")
public class Asset {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dramaId;
    private Long episodeId;
    private Long storyboardId;
    private Integer storyboardNum;
    private String name;
    private String description;
    private String type;
    private String category;
    private String url;
    private String thumbnailUrl;
    private String localPath;
    private Long fileSize;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Integer duration;
    private String format;
    private Long imageGenId;
    private Long videoGenId;
    private Boolean isFavorite;
    private Integer viewCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}

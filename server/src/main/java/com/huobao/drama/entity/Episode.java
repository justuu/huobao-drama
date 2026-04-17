package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("episodes")
public class Episode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dramaId;
    private Integer episodeNumber;
    private String title;
    private String content;
    private String scriptContent;
    private String description;
    private Integer duration;
    private String status;
    private String videoUrl;
    private String thumbnail;
    private Long imageConfigId;
    private Long videoConfigId;
    private Long audioConfigId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}

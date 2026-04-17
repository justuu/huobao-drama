package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dramas")
public class Drama {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String genre;
    private String style;
    private Integer totalEpisodes;
    private Integer totalDuration;
    private String status;
    private String thumbnail;
    private String tags;
    private String metadata;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}

package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("props")
public class Prop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dramaId;
    private String name;
    private String type;
    private String description;
    private String prompt;
    private String imageUrl;
    private String referenceImages;
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

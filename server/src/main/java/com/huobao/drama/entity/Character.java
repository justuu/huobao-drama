package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("characters")
public class Character {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dramaId;
    private String name;
    private String role;
    private String description;
    private String appearance;
    private String prompt;
    private String personality;
    private String voiceStyle;
    private String imageUrl;
    private String referenceImages;
    private String seedValue;
    private Integer sortOrder;
    private String localPath;
    private String voiceSampleUrl;
    private String voiceProvider;
    private String assetId;
    private String assetGroupId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}

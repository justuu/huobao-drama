package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seedance_assets")
public class SeedanceAsset {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long dramaId;
    private String refType;
    private Long refId;
    private String name;
    private String assetId;
    private String imageUrl;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

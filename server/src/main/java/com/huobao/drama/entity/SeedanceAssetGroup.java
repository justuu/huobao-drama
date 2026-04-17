package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seedance_asset_groups")
public class SeedanceAssetGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dramaId;
    private String groupName;
    private String groupType;
    private String seedanceGroupId;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

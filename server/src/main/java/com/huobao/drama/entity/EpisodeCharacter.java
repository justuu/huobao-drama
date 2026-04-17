package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("episode_characters")
public class EpisodeCharacter {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long episodeId;
    private Long characterId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

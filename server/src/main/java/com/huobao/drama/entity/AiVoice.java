package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_voices")
public class AiVoice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String voiceId;
    private String voiceName;
    private String description;
    private String language;
    private String provider;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

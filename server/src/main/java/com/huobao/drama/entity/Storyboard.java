package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("storyboards")
public class Storyboard {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long episodeId;
    private Long sceneId;
    private Integer storyboardNumber;
    private String title;
    private String location;
    private String time;
    private String shotType;
    private String angle;
    private String movement;
    private String action;
    private String result;
    private String atmosphere;
    private String videoPrompt;
    private String bgmPrompt;
    private String soundEffect;
    private String dialogue;
    private String description;
    private Integer duration;
    private String promptText;
    private Integer durationMs;
    private Integer groupIndex;
    private String groupId;
    private String referenceImages;
    private String videoUrl;
    private String ttsAudioUrl;
    private String subtitleUrl;
    private String composedVideoUrl;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private LocalDateTime deletedAt;
}

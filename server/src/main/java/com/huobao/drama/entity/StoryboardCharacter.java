package com.huobao.drama.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("storyboard_characters")
public class StoryboardCharacter {
    private Long storyboardId;
    private Long characterId;
}

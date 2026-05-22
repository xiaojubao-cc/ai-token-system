package com.ai.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_token_model")
public class ModelInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String modelName;

    private String modelCode;

    private String description;

    private LocalDateTime createTime;
}

package com.ai.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_token_apikey")
public class ApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String apikey;

    private Long modelId;

    private Integer useStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

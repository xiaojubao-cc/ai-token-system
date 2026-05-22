package com.ai.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ai_token_usage")
public class TokenUsage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String accountId;

    private String userId;

    private Long apikeyId;

    private Long tokens;

    private Long requestCount;

    private LocalDate recordDate;

    private LocalDateTime createTime;
}

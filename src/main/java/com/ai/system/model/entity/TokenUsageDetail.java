package com.ai.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_token_usage_detail")
public class TokenUsageDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 ai_token_usage.id */
    private Long usageId;

    /** 模型名称 */
    private String name;

    /** 张数 */
    private Long amount;

    /** 按张请求 */
    private Long amountRequest;

    private LocalDateTime createTime;
}

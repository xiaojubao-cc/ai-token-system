package com.ai.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_token_usage_detail_res_token")
public class TokenUsageDetailResToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 ai_token_usage_detail.id */
    private Long detailId;

    private String resolution;

    private Long videoModeOutputToken;

    private Long videoLessModeOutputToken;

    private Integer requestCount;
}

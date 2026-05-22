package com.ai.system.model.vo.token;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TokenUsageQueryVO {
    private Long apikeyId;
    @NotNull
    private Integer groupBy;
    @NotNull
    private String startTime;
    @NotNull
    private String endTime;
    private Long page;
    private Long pageSize;
}

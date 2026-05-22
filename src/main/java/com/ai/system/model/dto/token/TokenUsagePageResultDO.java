package com.ai.system.model.dto.token;

import lombok.Data;
import java.util.List;

@Data
public class TokenUsagePageResultDO {
    private Long total;
    private Long page;
    private Long pageSize;
    private List<TokenUsageDO> list;
}

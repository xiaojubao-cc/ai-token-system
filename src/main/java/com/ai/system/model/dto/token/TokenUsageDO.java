package com.ai.system.model.dto.token;

import lombok.Data;

@Data
public class TokenUsageDO {
    private String accountId;
    private String userId;
    private Long apikeyId;
    private String apikey;
    private String businessName;
    private String modelName;
    private String recordDate;
    private Long tokens;
    private Long request;
}

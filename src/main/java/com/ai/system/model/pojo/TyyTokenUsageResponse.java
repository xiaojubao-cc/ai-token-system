package com.ai.system.model.pojo;

import com.ai.system.model.dto.token.TokenUsageDO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TyyTokenUsageResponse {
    private List<TokenUsageDO> returnObj;;
    private String message;
    private Integer statusCode;

}

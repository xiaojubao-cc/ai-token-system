package com.ai.system.model.dto.apikey;

import lombok.Data;

@Data
public class ApikeyDO {
    private Long id;
    private Long userId;
    private String username;
    private String apikey;
    private Long modelId;
    private String modelName;
    private String modelCode;
    private Integer useStatus;
    private String createTime;
}

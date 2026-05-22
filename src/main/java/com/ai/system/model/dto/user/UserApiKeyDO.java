package com.ai.system.model.dto.user;

import lombok.Data;

@Data
public class UserApiKeyDO {
    private Long id;
    private Long userId;
    private String apikey;
    private Long modelId;
    private String modelName;
    private Integer status;
    private String createTime;
}

package com.ai.system.model.dto.user;

import lombok.Data;

@Data
public class UserApiKeyDO {
    private Long id;
    private Long userId;
    private String apikey;
    private String secretKey;
    private Integer status;
    private String createTime;
}

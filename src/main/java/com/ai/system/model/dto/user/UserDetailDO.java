package com.ai.system.model.dto.user;

import lombok.Data;

@Data
public class UserDetailDO {
    private Long id;
    private String username;
    private String businessName;
    private String email;
    private String phone;
    private String userId;
    private String accountId;
    private String accessKey;
    private String securityKey;
    private String role;
    private Integer status;
    private Long apiKeyCount;
    private String createTime;
}

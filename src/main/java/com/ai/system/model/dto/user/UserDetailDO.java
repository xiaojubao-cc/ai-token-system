package com.ai.system.model.dto.user;

import lombok.Data;

@Data
public class UserDetailDO {
    private Long id;
    private String username;
    private String businessName;
    private String email;
    private String phone;
    private String role;
    private Integer status;
    private Long apiKeyCount;
    private String associatedModels;
    private String createTime;
}

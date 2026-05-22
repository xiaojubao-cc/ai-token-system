package com.ai.system.controller.user.vo;

import lombok.Data;

@Data
public class UserUpdateVO {
    private Long id;
    private String username;
    private String businessName;
    private String password;
    private String email;
    private String phone;
    private String role;
    private Integer status;
}

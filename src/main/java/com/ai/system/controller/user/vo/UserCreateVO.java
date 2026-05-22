package com.ai.system.controller.user.vo;

import lombok.Data;

@Data
public class UserCreateVO {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String role;
}

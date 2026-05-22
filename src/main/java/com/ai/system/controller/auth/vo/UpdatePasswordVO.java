package com.ai.system.controller.auth.vo;

import lombok.Data;

@Data
public class UpdatePasswordVO {
    private String id;
    private String oldPassword;
    private String newPassword;
}

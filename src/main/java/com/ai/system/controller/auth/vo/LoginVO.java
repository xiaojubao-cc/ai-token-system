package com.ai.system.controller.auth.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginVO {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private Boolean rememberMe;
}

package com.ai.system.controller.auth.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutVO {
    @NotBlank
    private String refreshToken;
}

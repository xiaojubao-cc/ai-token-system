package com.ai.system.controller.user.vo;

import lombok.Data;

@Data
public class ApiKeyCreateVO {
    private String apikey;
    private Long modelId;
    private Integer status;
}

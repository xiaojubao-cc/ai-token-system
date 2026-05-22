package com.ai.system.controller.apikey.vo;

import lombok.Data;

@Data
public class ApiKeyItemVO {
    private Long id;
    private Long userId;
    private String apikey;
    private Integer useStatus;
}

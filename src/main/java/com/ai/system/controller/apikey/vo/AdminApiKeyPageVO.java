package com.ai.system.controller.apikey.vo;

import lombok.Data;

@Data
public class AdminApiKeyPageVO {
    private Long userId;
    private String startTime;
    private String endTime;
    private Long page;
    private Long pageSize;
}

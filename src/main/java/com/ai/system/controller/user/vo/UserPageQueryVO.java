package com.ai.system.controller.user.vo;

import lombok.Data;

@Data
public class UserPageQueryVO {
    private String apikey;
    private String username;
    private String startTime;
    private String endTime;
    private Long page;
    private Long pageSize;
}

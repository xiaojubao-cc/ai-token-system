package com.ai.system.model.dto.apikey;

import lombok.Data;

@Data
public class ApikeyDO {
    private Long id;
    private Long userId;
    private String username;
    private String businessName;
    private String apikey;
    private String secretKey;
    private Integer useStatus;
    private String createTime;
}

package com.ai.system.controller.apikey.vo;

import lombok.Data;
import java.util.List;

@Data
public class ApiKeyPageVO {
    private Long total;
    private Long page;
    private Long pageSize;
    private List<ApiKeyItemVO> list;
}

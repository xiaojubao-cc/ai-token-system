package com.ai.system.model.dto.apikey;

import lombok.Data;
import java.util.List;

@Data
public class ApikeyPageResultDO {
    private Long total;
    private Long page;
    private Long pageSize;
    private List<ApikeyDO> list;
}

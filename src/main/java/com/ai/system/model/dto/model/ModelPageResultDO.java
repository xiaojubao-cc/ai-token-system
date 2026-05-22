package com.ai.system.model.dto.model;

import com.ai.system.model.entity.ModelInfo;
import lombok.Data;

import java.util.List;

@Data
public class ModelPageResultDO {

    private Long total;

    private Long page;

    private Long pageSize;

    private List<ModelInfo> list;
}

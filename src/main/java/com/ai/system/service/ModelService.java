package com.ai.system.service;

import com.ai.system.model.dto.model.ModelPageResultDO;
import com.ai.system.model.entity.ModelInfo;

import java.util.List;

public interface ModelService {

    List<ModelInfo> listAll();

    ModelPageResultDO pageQuery(String keyword, Long page, Long pageSize);

    ModelInfo getById(Long id);

    void create(ModelInfo model);

    void update(ModelInfo model);

    void delete(Long id);
}

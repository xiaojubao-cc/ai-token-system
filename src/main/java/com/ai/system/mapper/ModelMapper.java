package com.ai.system.mapper;

import com.ai.system.model.entity.ModelInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModelMapper extends BaseMapper<ModelInfo> {
}

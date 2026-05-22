package com.ai.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ai.system.mapper.ModelMapper;
import com.ai.system.model.dto.model.ModelPageResultDO;
import com.ai.system.model.entity.ModelInfo;
import com.ai.system.service.ModelService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModelServiceImpl implements ModelService {

    @Resource
    private ModelMapper modelMapper;

    @Override
    public List<ModelInfo> listAll() {
        return modelMapper.selectList(new LambdaQueryWrapper<ModelInfo>()
                .orderByAsc(ModelInfo::getCreateTime));
    }

    @Override
    public ModelPageResultDO pageQuery(String keyword, Long page, Long pageSize) {
        LambdaQueryWrapper<ModelInfo> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(ModelInfo::getModelName, keyword)
                    .or()
                    .like(ModelInfo::getModelCode, keyword));
        }
        wrapper.orderByAsc(ModelInfo::getCreateTime);
        Page<ModelInfo> result = modelMapper.selectPage(new Page<>(page, pageSize), wrapper);

        ModelPageResultDO pageResult = new ModelPageResultDO();
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(result.getCurrent());
        pageResult.setPageSize(result.getSize());
        pageResult.setList(result.getRecords());
        return pageResult;
    }

    @Override
    public ModelInfo getById(Long id) {
        return modelMapper.selectById(id);
    }

    @Override
    public void create(ModelInfo model) {
        modelMapper.insert(model);
    }

    @Override
    public void update(ModelInfo model) {
        modelMapper.updateById(model);
    }

    @Override
    public void delete(Long id) {
        modelMapper.deleteById(id);
    }
}

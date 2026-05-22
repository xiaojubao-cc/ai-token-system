package com.ai.system.controller.model;

import com.ai.system.model.dto.model.ModelPageResultDO;
import com.ai.system.model.entity.ModelInfo;
import com.ai.system.model.pojo.CommonResult;
import com.ai.system.service.ModelService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/models")
public class ModelController {

    @Resource
    private ModelService modelService;

    /**
     * 分页查询模型列表
     */
    @GetMapping("/page")
    public CommonResult<ModelPageResultDO> page(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword) {
        return CommonResult.success(modelService.pageQuery(keyword, page, pageSize));
    }

    /**
     * 查询全部模型列表（下拉选择用）
     */
    @GetMapping
    public CommonResult<List<ModelInfo>> list() {
        return CommonResult.success(modelService.listAll());
    }

    /**
     * 新增模型
     */
    @PostMapping("/create")
    public CommonResult<Boolean> create(@RequestBody ModelInfo model) {
        modelService.create(model);
        return CommonResult.success(true);
    }

    /**
     * 更新模型
     */
    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody ModelInfo model) {
        modelService.update(model);
        return CommonResult.success(true);
    }

    /**
     * 删除模型
     */
    @PostMapping("/delete")
    public CommonResult<Boolean> delete(@RequestBody ModelInfo model) {
        modelService.delete(model.getId());
        return CommonResult.success(true);
    }
}

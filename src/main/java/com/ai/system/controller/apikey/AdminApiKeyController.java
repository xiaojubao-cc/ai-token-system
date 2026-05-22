package com.ai.system.controller.apikey;

import com.ai.system.model.dto.apikey.ApikeyPageResultDO;
import com.ai.system.model.pojo.CommonResult;
import com.ai.system.controller.apikey.vo.AdminApiKeyPageVO;
import com.ai.system.service.ApiKeyService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/apikey")
public class AdminApiKeyController {

    @Resource
    private ApiKeyService apiKeyService;

    @GetMapping("/page")
    public CommonResult<ApikeyPageResultDO> page(AdminApiKeyPageVO query) {
        if (query.getPage() == null) query.setPage(1L);
        if (query.getPageSize() == null) query.setPageSize(10L);
        return CommonResult.success(apiKeyService.adminPageQuery(query));
    }
}

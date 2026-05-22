package com.ai.system.service;

import com.ai.system.controller.apikey.vo.AdminApiKeyPageVO;
import com.ai.system.model.dto.apikey.ApikeyPageResultDO;

public interface ApiKeyService {
    public ApikeyPageResultDO adminPageQuery(AdminApiKeyPageVO query);

    public ApikeyPageResultDO userPageQuery(Long userId, Long page, Long pageSize);

}

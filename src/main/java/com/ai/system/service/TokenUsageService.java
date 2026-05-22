package com.ai.system.service;

import com.ai.system.model.dto.token.TokenUsageDO;
import com.ai.system.model.dto.token.TokenUsagePageResultDO;

import java.util.List;

public interface TokenUsageService {

    TokenUsagePageResultDO adminQuery(Long userId, Long apikeyId, Integer groupBy,
                                      String startTime, String endTime, Long page, Long pageSize);

    TokenUsagePageResultDO userQuery(Long userId, Long apikeyId, Integer groupBy,
                                     String startTime, String endTime, Long page, Long pageSize);

    /** 不分页，用于 Dashboard 统计 */
    List<TokenUsageDO> adminQueryAll(Long userId, Long apikeyId, Integer groupBy,
                                     String startTime, String endTime);

    List<TokenUsageDO> userQueryAll(Long userId, Long apikeyId, Integer groupBy,
                                    String startTime, String endTime);

    void syncTodayData();
}

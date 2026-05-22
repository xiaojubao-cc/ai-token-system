package com.ai.system.task;

import com.ai.system.service.impl.TokenUsageServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TokenUsageSyncTask {

    @Resource
    private TokenUsageServiceImpl tokenUsageService;

    /**
     * 每天晚上 23:59:59 执行，将当天 Token 用量同步入库
     */
    @Scheduled(cron = "59 59 23 * * ?")
    public void syncTokenUsage() {
        log.info("【Token 用量定时任务】触发执行");
        tokenUsageService.syncTodayData();
    }
}

package com.ai.system.service.impl;

import com.ai.system.mapper.ApiKeyMapper;
import com.ai.system.model.dto.token.TokenUsageDO;
import com.ai.system.controller.dashboard.vo.DashboardStatsDO;
import com.ai.system.model.entity.ApiKey;
import com.ai.system.service.DashboardService;
import com.ai.system.service.TokenUsageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private TokenUsageService tokenUsageService;

    @Resource
    private ApiKeyMapper apiKeyMapper;

    @Override
    public DashboardStatsDO getAdminStats(String startTime, String endTime) {
        List<TokenUsageDO> allData = tokenUsageService.adminQueryAll(
                null, null, 0, startTime, endTime);
        return buildAdminStats(allData);
    }

    @Override
    public DashboardStatsDO getUserStats(Long userId, String startTime, String endTime) {
        List<TokenUsageDO> allData = tokenUsageService.userQueryAll(
                userId, null, 0, startTime, endTime);
        return buildUserStats(allData);
    }

    private DashboardStatsDO buildAdminStats(List<TokenUsageDO> data) {
        DashboardStatsDO stats = new DashboardStatsDO();

        long totalTokens = data.stream().mapToLong(TokenUsageDO::getTokens).sum();
        long totalInputTokens = data.stream().mapToLong(d -> d.getInputTokens() != null ? d.getInputTokens() : 0L).sum();
        long totalOutputTokens = data.stream().mapToLong(d -> d.getOutputTokens() != null ? d.getOutputTokens() : 0L).sum();
        long totalRequests = data.stream().mapToLong(TokenUsageDO::getRequest).sum();
        // 按 apikey 所属的系统用户 ID 去重，统计活跃用户数
        Set<Long> apikeyIds = data.stream()
                .map(TokenUsageDO::getApikeyId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> activeUserIds = new HashSet<>();
        if (!apikeyIds.isEmpty()) {
            List<ApiKey> keys = apiKeyMapper.selectBatchIds(apikeyIds);
            keys.stream().map(ApiKey::getUserId).filter(Objects::nonNull).forEach(activeUserIds::add);
        }

        stats.setTotalTokens(totalTokens);
        stats.setTotalInputTokens(totalInputTokens);
        stats.setTotalOutputTokens(totalOutputTokens);
        stats.setTotalRequests(totalRequests);
        stats.setActiveUsers((long) activeUserIds.size());

        // 趋势按 recordDate 聚合
        Map<String, Long> trendMap = new LinkedHashMap<>();
        for (TokenUsageDO vo : data) {
            String date = vo.getRecordDate() != null ? vo.getRecordDate() : "";
            trendMap.merge(date, vo.getTokens() != null ? vo.getTokens() : 0L, Long::sum);
        }
        List<DashboardStatsDO.TrendPoint> trendData = new ArrayList<>();
        for (Map.Entry<String, Long> e : trendMap.entrySet()) {
            DashboardStatsDO.TrendPoint point = new DashboardStatsDO.TrendPoint();
            point.setDate(e.getKey());
            point.setTokens(e.getValue());
            trendData.add(point);
        }
        trendData.sort(Comparator.comparing(DashboardStatsDO.TrendPoint::getDate));
        stats.setTrendData(trendData);

        // 按用户（businessName）聚合分布
        Map<String, Long> distMap = new LinkedHashMap<>();
        for (TokenUsageDO vo : data) {
            String name = vo.getBusinessName() != null ? vo.getBusinessName() : "Key #" + vo.getApikeyId();
            distMap.merge(name, vo.getTokens() != null ? vo.getTokens() : 0L, Long::sum);
        }
        List<DashboardStatsDO.DistItem> distribution = new ArrayList<>();
        for (Map.Entry<String, Long> e : distMap.entrySet()) {
            DashboardStatsDO.DistItem item = new DashboardStatsDO.DistItem();
            item.setName(e.getKey());
            item.setValue(e.getValue());
            distribution.add(item);
        }
        distribution.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        stats.setDistribution(distribution);

        return stats;
    }

    private DashboardStatsDO buildUserStats(List<TokenUsageDO> data) {
        DashboardStatsDO stats = new DashboardStatsDO();

        long totalTokens = data.stream().mapToLong(TokenUsageDO::getTokens).sum();
        long totalInputTokens = data.stream().mapToLong(d -> d.getInputTokens() != null ? d.getInputTokens() : 0L).sum();
        long totalOutputTokens = data.stream().mapToLong(d -> d.getOutputTokens() != null ? d.getOutputTokens() : 0L).sum();
        long totalRequests = data.stream().mapToLong(TokenUsageDO::getRequest).sum();
        long activeKeyCount = data.stream()
                .map(TokenUsageDO::getApikeyId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        stats.setTotalTokens(totalTokens);
        stats.setTotalInputTokens(totalInputTokens);
        stats.setTotalOutputTokens(totalOutputTokens);
        stats.setTotalRequests(totalRequests);
        stats.setActiveUsers(activeKeyCount);

        // 趋势按 recordDate 聚合
        Map<String, Long> trendMap = new LinkedHashMap<>();
        for (TokenUsageDO vo : data) {
            String date = vo.getRecordDate() != null ? vo.getRecordDate() : "";
            trendMap.merge(date, vo.getTokens() != null ? vo.getTokens() : 0L, Long::sum);
        }
        List<DashboardStatsDO.TrendPoint> trendData = new ArrayList<>();
        for (Map.Entry<String, Long> e : trendMap.entrySet()) {
            DashboardStatsDO.TrendPoint point = new DashboardStatsDO.TrendPoint();
            point.setDate(e.getKey());
            point.setTokens(e.getValue());
            trendData.add(point);
        }
        trendData.sort(Comparator.comparing(DashboardStatsDO.TrendPoint::getDate));
        stats.setTrendData(trendData);

        // 按 API Key 聚合分布，展示完整 apikey
        Map<String, Long> distMap = new LinkedHashMap<>();
        for (TokenUsageDO vo : data) {
            if (vo.getApikeyId() == null) continue;
            String key = vo.getApikey() != null ? vo.getApikey() : "Key #" + vo.getApikeyId();
            distMap.merge(key, vo.getTokens() != null ? vo.getTokens() : 0L, Long::sum);
        }
        List<DashboardStatsDO.DistItem> distribution = new ArrayList<>();
        for (Map.Entry<String, Long> e : distMap.entrySet()) {
            DashboardStatsDO.DistItem item = new DashboardStatsDO.DistItem();
            item.setName(e.getKey());
            item.setValue(e.getValue());
            distribution.add(item);
        }
        distribution.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        stats.setDistribution(distribution);

        return stats;
    }
}

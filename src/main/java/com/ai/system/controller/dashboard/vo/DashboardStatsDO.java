package com.ai.system.controller.dashboard.vo;

import lombok.Data;
import java.util.List;

@Data
public class DashboardStatsDO {
    private Long totalTokens;
    private Long totalInputTokens;
    private Long totalOutputTokens;
    private Long totalRequests;
    private Long activeUsers;
    private List<TrendPoint> trendData;
    private List<DistItem> distribution;

    @Data
    public static class TrendPoint {
        private String date;
        private Long tokens;
    }

    @Data
    public static class DistItem {
        private String name;
        private Long value;
    }
}

package com.ai.system.model.dto.token;

import lombok.Data;

import java.util.List;

@Data
public class TokenUsageDO {
    private String accountId;
    private String userId;
    private Long apikeyId;
    private String apikey;
    private String businessName;
    private String recordDate;
    private Long tokens;
    private Long inputTokens;
    private Long outputTokens;
    private Long request;
    private Long totalDuration;
    private Long totalAmount;
    private List<DetailItem> detail;

    @Data
    public static class DetailItem {
        private String name;
        private List<StageItem> stage;
        private Long amount;
        private Long amountRequest;
        private List<ResolutionDurationItem> resolutionDuration;
        private List<ResolutionTokenItem> resolutionToken;
    }

    @Data
    public static class StageItem {
        private Long inputTokens;
        private Long outputTokens;
        private Long minContext;
        private Long maxContext;
    }

    @Data
    public static class ResolutionDurationItem {
        private String resolution;
        private Long cnt;
        private Integer requestCount;
    }

    @Data
    public static class ResolutionTokenItem {
        private String resolution;
        private Long videoModeOutputToken;
        private Long videoLessModeOutputToken;
        private Integer requestCount;
    }
}

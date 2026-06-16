package com.ai.system.model.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TyyApiKeyResponse {
    private TyyApiKeyReturnObj returnObj;
    private String message;
    private Integer statusCode;


    @Data
    public static class TyyApiKeyReturnObj {
        private String apikey;
        private Long id;
    }
}


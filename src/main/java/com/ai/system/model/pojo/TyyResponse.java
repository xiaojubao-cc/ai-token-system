package com.ai.system.model.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TyyResponse {
    private String body;
    private Integer statusCode;
    private Map<String,Object> returnObj;
    private Map<String,String> headers;
}

package com.ai.system.model.pojo;

import lombok.Data;

import java.util.Map;

/**
 * 通用HTTP响应封装
 */
@Data
public class Response {
    private String body;
    private String message;
    private Integer statusCode;
    private Map<String, String> returnObj;
    private Map<String, String> headers;
}

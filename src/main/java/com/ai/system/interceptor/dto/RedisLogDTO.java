package com.ai.system.interceptor.dto;

import lombok.Data;

@Data
public class RedisLogDTO {
    /**
     * 调用类名
     */
    private String clazz;

    /**
     * 调用方法名
     */
    private String method;

    /**
     * 调用的参数（包括键和值）
     */
    private String params;

    /**
     * Redis 命令类型 (SET, GET, HSET, HGET, DEL 等)
     */
    private String command;

    /**
     * 操作的键名
     */
    private String key;

    /**
     * 操作的值（敏感信息需要脱敏）
     */
    private String value;

    /**
     * 方法返回结果
     */
    private Object result;

    /**
     * 执行时长，毫秒
     */
    private long costTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 出现的异常
     */
    private Exception exp;
    @Override
    public String toString() {
        return "redis execute：{method=[" + method +
                "], parm=" + params +
                ", result=[" + result +
                "], costTime=[" + costTime +
                "], remark=[" + remark +
                "], exp=[" + exp +
                "]}";

    }

}

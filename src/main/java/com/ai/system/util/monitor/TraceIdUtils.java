package com.ai.system.util.monitor;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

public class TraceIdUtils {

    public static final String TRACE_ID = "traceId";

    public static String getTraceId(){
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void setTraceId(HttpServletRequest request){
        //如果有上层调用就用上层的ID
        String traceId = request.getHeader(TraceIdUtils.TRACE_ID);
        if (traceId == null) {
            traceId = TraceIdUtils.getTraceId();
        }

        MDC.put(TraceIdUtils.TRACE_ID, traceId);
    }


    public static void putTraceIdIfAbsent() {
        if (MDC.get(TraceIdUtils.TRACE_ID) == null) {
            MDC.put(TraceIdUtils.TRACE_ID, TraceIdUtils.getTraceId());
        }
    }

    public static void remove(){
        MDC.remove(TraceIdUtils.TRACE_ID);
    }


    /**
     * 将父线程的MDC绑定 到子线程
     * @param context
     */
    public static void setParentMdcToChildMdc(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
        putTraceIdIfAbsent();
    }
}

package com.ai.system.config.proceesor;


import com.ai.system.interceptor.RedisLogInterceptor;
import com.ai.system.util.monitor.TraceIdUtils;
import com.alibaba.ttl.TtlRunnable;
import org.slf4j.MDC;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 后置处理器
 */
@Component
public class AITokenBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        //线程池的增强
        if(bean instanceof ThreadPoolTaskExecutor){
            // 修改提交的任务，接入 TransmittableThreadLocal
            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) bean;
            // 使用组合装饰器，先处理TTL，再处理TradeId
            executor.setTaskDecorator(new TraceIdAwareTtlTaskDecorator());
            return executor;
        }
        // redis增强
        if ("redisConnectionFactory".equals(beanName)) {
            //设置代理对象
            ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setTarget(bean);
            proxyFactory.setProxyTargetClass(true);
            proxyFactory.addAdvice(new RedisLogInterceptor());
            return proxyFactory.getProxy();
        }
        return bean;
    }

    /**
     * 自定义 TaskDecorator：
     * 1. 用 TtlRunnable 传递 TransmittableThreadLocal（含 traceId）；
     * 2. 子线程执行前后同步 traceId 到 MDC，确保日志打印。
     */
    private static class TraceIdAwareTtlTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // 步骤1：捕获主线程当前的 MDC 上下文（兜底：若主线程已手动设置 MDC，直接复用）
            Map<String, String> parentMdcContext = MDC.getCopyOfContextMap();

            // 步骤2：用 TtlRunnable 包装任务，确保 traceId（TTL 变量）跨线程传递
            Runnable ttlRunnable = TtlRunnable.get(runnable);

            // 步骤3：返回装饰后的 Runnable，在子线程中处理 MDC 同步
            return () -> {
                // 备份子线程（线程池核心线程）原有的 MDC 状态，避免污染
                Map<String, String> childPrevMdc = MDC.getCopyOfContextMap();
                try {
                    // 情况A：若主线程 MDC 已有 traceId，直接复用（兜底逻辑）
                    if (parentMdcContext != null && parentMdcContext.containsKey(TraceIdUtils.TRACE_ID)) {
                        MDC.setContextMap(parentMdcContext);
                    }
                    // 情况B：从 TTL 变量（TraceIdUtils）中获取 traceId，写入 MDC（核心逻辑）
                    else {
                        String traceId = TraceIdUtils.getTraceId(); // 从 TTL 中获取（自动生成/继承主线程）
                        MDC.put(TraceIdUtils.TRACE_ID, traceId);  // 写入 MDC，供日志打印
                    }

                    // 步骤4：执行实际任务（此时子线程 MDC 已包含 traceId）
                    ttlRunnable.run();
                } finally {
                    // 步骤5：恢复子线程原有 MDC 状态，避免线程池复用导致 traceId 串用
                    if (childPrevMdc != null) {
                        MDC.setContextMap(childPrevMdc);
                    } else {
                        MDC.clear();
                    }
                }
            };
        }
    }
}

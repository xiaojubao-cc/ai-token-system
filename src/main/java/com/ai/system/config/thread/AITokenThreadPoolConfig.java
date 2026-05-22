package com.ai.system.config.thread;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;


@Configuration
@ConfigurationProperties(prefix = "thread-pool")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AITokenThreadPoolConfig {
    private PoolConfig master;
    private PoolConfig slave;
    private PoolConfig other;

    /**
     * 主线程
     * @return
     */
    @Bean(name = "masterThreadPool")
    public ThreadPoolTaskExecutor masterThreadPool() {
        return createThreadPool(master);
    }

    private ThreadPoolTaskExecutor createThreadPool(PoolConfig config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCorePoolSize());
        executor.setMaxPoolSize(config.getMaxPoolSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        executor.setThreadNamePrefix(config.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoolConfig {
        private int corePoolSize;
        private int maxPoolSize;
        private int queueCapacity;
        private int keepAliveSeconds;
        private String threadNamePrefix;
    }
}

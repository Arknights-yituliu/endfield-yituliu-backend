package org.yituliu.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableAsync // 启用异步支持
public class AsyncConfig {

    /**
     * 定义共享线程池
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        // 线程工厂，用于创建有意义名称的线程
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);
            
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "async-executor-" + counter.incrementAndGet());
            }
        };
        
        // 创建线程池
        return new ThreadPoolExecutor(
                4,              // 核心线程数
                8,              // 最大线程数
                30L,            // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),  // 队列容量
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        );
    }
}


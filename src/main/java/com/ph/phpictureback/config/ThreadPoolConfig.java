package com.ph.phpictureback.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.*;

@Configuration
@Slf4j
public class ThreadPoolConfig {

    /**
     * 图片点赞同步线程池
     */
    @Bean("SyncExecutorService")
    public ExecutorService pictureSyncExecutor() {
        // 核心参数（根据服务器配置和业务调整）
        int corePoolSize = 5; // 核心线程数（常驻线程，即使空闲也不销毁）
        int maximumPoolSize = 10; // 最大线程数（核心线程忙不过来时，临时创建的线程上限）
        long keepAliveTime = 60; // 临时线程空闲时间（超过这个时间就销毁）
        TimeUnit unit = TimeUnit.SECONDS;
        
        // 任务队列（当所有线程都繁忙时，新任务会先进入队列等待）
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(1000);
        
        // 线程工厂（自定义线程名称，便于日志排查）
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("picture-sync-thread-" + count++);
                return thread;
            }
        };
        
        // 拒绝策略（当队列满了且达到最大线程数时，如何处理新任务）
        // 这里选择"记录日志并放弃任务"，也可根据业务选择重试或交给主线程执行
        RejectedExecutionHandler handler = (r, executor) -> {
            log.error("图片点赞同步任务被拒绝，任务队列已满");
        };
        
        return new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            workQueue,
            threadFactory,
            handler
        );
    }
}
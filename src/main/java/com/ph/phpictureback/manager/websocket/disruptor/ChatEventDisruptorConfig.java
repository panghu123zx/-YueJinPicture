package com.ph.phpictureback.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class ChatEventDisruptorConfig {

    @Resource
    private ChatEventWorkHandler chatEventWorkHandler;

    @Bean("chatEventDisruptor")
    public Disruptor<ChatEvent> messageModelRingBuffer() {
        // ringBuffer 的大小
        int bufferSize = 1024 * 256;
        Disruptor<ChatEvent> disruptor = new Disruptor<>(
                ChatEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("chatEventDisruptor").build()
        );
        // 设置消费者
        disruptor.handleEventsWithWorkerPool(chatEventWorkHandler);
        // 开启 disruptor
        disruptor.start();
        return disruptor;
    }
}

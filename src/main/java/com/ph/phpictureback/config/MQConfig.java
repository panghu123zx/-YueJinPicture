package com.ph.phpictureback.config;

import com.ph.phpictureback.constant.RabbitMqAiConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 死信交换机
 */
@Configuration
public class MQConfig {
    /**
     * AI业务交换机
     */
    @Bean
    public DirectExchange aiExchange() {
        // 参数：交换机名、持久化、自动删除
        return new DirectExchange(RabbitMqAiConstant.AI_EXCHANGE, true, false);
    }

    /**
     * AI业务队列（绑定死信交换机）
     */
    @Bean
    public Queue aiQueue() {
        Map<String, Object> args = new HashMap<>();
        // 死信配置：消息成为死信后，转发到DLX交换机+DLX路由键
        args.put("x-dead-letter-exchange", RabbitMqAiConstant.DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", RabbitMqAiConstant.DLX_ROUTING);
        args.put("x-message-ttl", 60000); // 消息60秒后过期
        // 队列属性：持久化、非独占、非自动删除
        return new Queue(RabbitMqAiConstant.AI_QUEUE, true, false, false, args);
    }

    /**
     * AI队列与AI交换机的绑定（业务路由）
     */
    @Bean
    public Binding aiBinding(Queue aiQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(aiQueue)
                .to(aiExchange)
                .with(RabbitMqAiConstant.AI_ROUTING); // 业务路由键
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(RabbitMqAiConstant.DLX_EXCHANGE, true, false);
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue dlxQueue() {
        // 死信队列通常也需要持久化，避免死信丢失
        return new Queue(RabbitMqAiConstant.DLX_QUEUE, true, false, false);
    }

    /**
     * 死信队列与死信交换机的绑定
     */
    @Bean
    public Binding dlxBinding(Queue dlxQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlxQueue)
                .to(dlxExchange)
                .with(RabbitMqAiConstant.DLX_ROUTING); // 死信路由键
    }

}

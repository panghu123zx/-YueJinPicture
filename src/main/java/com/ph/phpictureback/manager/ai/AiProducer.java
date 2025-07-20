package com.ph.phpictureback.manager.ai;

import com.ph.phpictureback.constant.RabbitMqAiConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * ai消息的生产者
 */
@Component
public class AiProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息给ai
     * @param message
     */
    public void sendAiMessage(String message){
        rabbitTemplate.convertAndSend(RabbitMqAiConstant.AI_EXCHANGE,RabbitMqAiConstant.AI_ROUTING, message);
    }
}

package com.ph.phpictureback.constant;

public interface RabbitMqAiConstant {
    /**
     * ai交换机
     */
    String AI_EXCHANGE="ai_exchange";
    /**
     * ai队列
     */
    String AI_QUEUE="ai_queue";
    /**
     * ai路由
     */
    String AI_ROUTING="ai_routing";

    /**
     * 图片ai交换机
     */
    String AI_PICTURE_EXCHANGE="ai_picture_exchange";
    /**
     * 图片ai队列
     */
    String AI_PICTURE_QUEUE="ai_picture_queue";
    /**
     * 图片ai路由
     */
    String AI_PICTURE_ROUTING="ai_picture_routing";


    /**
     * 死信交换机
     */
    String DLX_EXCHANGE = "dead_exchange";
    /**
     * 死信队列
     */
    String DLX_QUEUE = "dead_queue";
    /**
     * 死信路由
     */
    String DLX_ROUTING = "dead_routing";
}

package com.ph.phpictureback.manager.ai;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.constant.RabbitMqAiConstant;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.websocket.ChatHandler;
import com.ph.phpictureback.manager.websocket.model.ChatMessageTypeEnum;
import com.ph.phpictureback.manager.websocket.model.ChatResponseMessage;
import com.ph.phpictureback.model.entry.ChatMessage;
import com.ph.phpictureback.model.vo.ChatMessageVO;
import com.ph.phpictureback.service.ChatMessageService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 死信消费者
 */
@Component
@Slf4j
public class AiFailConsumer {

    @Resource
    private ChatMessageService chatMessageService;
    @Resource
    private ChatHandler chatHandler;
    /**
     * 监听死信队列
     *
     * @param message
     * @param channel
     * @param deliveryTag
     */
    @SneakyThrows
    @RabbitListener(queues = {RabbitMqAiConstant.DLX_QUEUE}, ackMode = "MANUAL")
    public void receiveFailMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) Long deliveryTag) {
        log.info("死信队列接收到的消息:{}", message);
        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息为空");
        }
        //消息是发送者消息的id
        Long id = Long.valueOf(message);
        ChatMessage chatMessage = chatMessageService.getById(id);

        Long chatId = chatMessage.getChatPromptId();
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setContent("ai对话发生了错误");
        aiMessage.setSendId(chatMessage.getReceiveId());
        aiMessage.setReplayId(id);
        aiMessage.setReceiveId(chatMessage.getSendId());
        aiMessage.setChatPromptId(chatId);
        boolean save = chatMessageService.save(aiMessage);
        if(!save){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "消息保存失败");
        }
        //重新广播消息
        Page<ChatMessageVO> historyMessages = chatMessageService.getHistoryMessages(chatId, 1L, 20L);
        ChatResponseMessage chatResponseMessage = new ChatResponseMessage();
        chatResponseMessage.setId(aiMessage.getId());
        chatResponseMessage.setType(ChatMessageTypeEnum.SEND.getValue());
        chatResponseMessage.setContent(aiMessage.getContent());
        chatResponseMessage.setTimestamp(aiMessage.getCreateTime());
//        chatResponseMessage.setUser(userService.getUserVo(user));
        chatResponseMessage.setHistoryMessage(historyMessages);

        chatHandler.broadcastToRoom(chatId, chatResponseMessage, null);

        channel.basicAck(deliveryTag,false);
    }
}

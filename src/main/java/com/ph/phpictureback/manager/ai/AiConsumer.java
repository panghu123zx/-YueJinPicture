package com.ph.phpictureback.manager.ai;

import cn.hutool.core.util.StrUtil;
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
import com.ph.phpictureback.service.UserService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * ai消息的消费者
 */
@Slf4j
@Component
public class AiConsumer {

    @Resource
    private ChatHandler chatHandler;
    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private AIManager aiManager;

    /**
     * 调用ai处理消息
     * @param message
     */
    @SneakyThrows
    @RabbitListener(queues = RabbitMqAiConstant.AI_QUEUE,ackMode = "MANUAL")
    public void receiveAiMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        //消息为空时
        if(StrUtil.isBlank(message)){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"消息为空");
        }
        //消息是发送者消息的id
        Long id = Long.valueOf(message);
        ChatMessage chatMessage = chatMessageService.getById(id);
        ThrowUtils.throwIf(chatMessage == null, ErrorCode.PARAMS_ERROR, "消息不存在");
        String content = chatMessage.getContent();
        if(chatMessage.getMessageType()!=null){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不支持发送图片文件");
        }
        if(StrUtil.isBlank(content)){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"内容为空");
        }

        //调用ai处理消息
        Long chatId = chatMessage.getChatPromptId();
        String res = aiManager.sendMsgToAI(content);
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setContent(res);
        aiMessage.setSendId(chatMessage.getReceiveId());
        aiMessage.setReplayId(id);
        aiMessage.setReceiveId(chatMessage.getSendId());
        aiMessage.setChatPromptId(chatId);
        boolean save = chatMessageService.save(aiMessage);
        if(!save){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "消息保存失败");
        }
        chatMessageService.updateChatCache(chatId,aiMessage);
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

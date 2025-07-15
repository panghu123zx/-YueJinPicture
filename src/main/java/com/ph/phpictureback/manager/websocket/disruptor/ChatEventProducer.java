package com.ph.phpictureback.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.ph.phpictureback.manager.websocket.model.ChatRequestMessage;
import com.ph.phpictureback.manager.websocket.model.PictureEditRequestMessage;
import com.ph.phpictureback.model.entry.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * 生产者（发布事件）
 */
@Component
@Slf4j
public class ChatEventProducer {
    @Resource
    Disruptor<ChatEvent> chatEventDisruptor;

    public void publishEvent(ChatRequestMessage chatRequestMessage, WebSocketSession session, User user, Long chatId) {
        RingBuffer<ChatEvent> ringBuffer = chatEventDisruptor.getRingBuffer();
        //获取可以生成的位置
        long next = ringBuffer.next();
        ChatEvent chatEvent = ringBuffer.get(next);
        chatEvent.setChatId(chatId);
        chatEvent.setUser(user);
        chatEvent.setChatRequestMessage(chatRequestMessage);
        chatEvent.setSession(session);
        //发布事件
        ringBuffer.publish(next);
    }


    /**
     * 优雅停机
     */
    @PreDestroy
    public void close() {
        chatEventDisruptor.shutdown();
    }

}

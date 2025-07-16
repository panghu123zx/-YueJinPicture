package com.ph.phpictureback.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.websocket.disruptor.ChatEventProducer;
import com.ph.phpictureback.manager.websocket.model.ChatMessageTypeEnum;
import com.ph.phpictureback.manager.websocket.model.ChatRequestMessage;
import com.ph.phpictureback.manager.websocket.model.ChatResponseMessage;
import com.ph.phpictureback.model.entry.ChatMessage;
import com.ph.phpictureback.model.entry.ChatPrompt;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.ChatMessageVO;
import com.ph.phpictureback.model.vo.UserVO;
import com.ph.phpictureback.service.ChatMessageService;
import com.ph.phpictureback.service.ChatPromptService;
import com.ph.phpictureback.service.UserService;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时聊天WebSocket处理器
 */
@Component
public class ChatHandler extends TextWebSocketHandler {
    @Resource
    private UserService userService;

    @Resource
    private ChatMessageService chatMessageService;
    @Resource
    private ChatPromptService chatPromptService;

    @Resource
    @Lazy
    private ChatEventProducer chatEventProducer;

    // 房间ID -> 该房间内的所有会话
    private final Map<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    // 用户的ID -> 该房间内的在线用户ID
    private final Map<Long, Set<Long>> userSession = new ConcurrentHashMap<>();


    /**
     * 连接建立后执行
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = (User) session.getAttributes().get("user");
        Long chatId = (Long) session.getAttributes().get("chatId");
        // 添加会话到房间
        roomSessions.putIfAbsent(chatId, ConcurrentHashMap.newKeySet());
        roomSessions.get(chatId).add(session);
        // 保存用户的session
        userSession.putIfAbsent(chatId, ConcurrentHashMap.newKeySet());
        userSession.get(chatId).add(user.getId());

        // 1. 向新连接用户发送历史消息
        sendHistoryMessages(session, chatId);

        // 3. 广播更新在线用户列表  todo
        broadcastOnlineUsers(chatId);
    }


    /**
     * 处理收到的消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        User user = (User) session.getAttributes().get("user");
        Long chatId = (Long) session.getAttributes().get("chatId");

        // 解析消息
        ChatRequestMessage requestMessage = JSONUtil.toBean(message.getPayload(), ChatRequestMessage.class);
        String content = requestMessage.getContent();
        Integer messageType = requestMessage.getMessageType();
        if (StrUtil.isBlank(content) && messageType==null) {
            return;
        }
        // 交给Disruptor处理（异步化）
        chatEventProducer.publishEvent(requestMessage, session, user, chatId);
    }


    /**
     * 连接关闭后执行
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        User user = (User) session.getAttributes().get("user");
        Long chatId = (Long) session.getAttributes().get("chatId");

        // 移除会话
        Set<WebSocketSession> sessions = roomSessions.get(chatId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(chatId);
            }
        }

        // 移除在线用户
        Set<Long> onlineUsers = userSession.get(chatId);
        if (onlineUsers != null) {
            onlineUsers.remove(user.getId());
            if (onlineUsers.isEmpty()) {
                userSession.remove(chatId);
            }
        }

        // 广播更新在线用户列表
        broadcastOnlineUsers(chatId);
    }


    /**
     * 发送历史消息给新连接用户
     */
    public void sendHistoryMessages(WebSocketSession session, Long chatId) throws IOException {
        // 获取历史消息（这里假设获取最近20条）
        Page<ChatMessageVO> historyMessages = chatMessageService.getHistoryMessages(chatId, 1L, 20L);
        //处理历史消息
        ChatResponseMessage response = new ChatResponseMessage();
        response.setType(ChatMessageTypeEnum.HISTORY.getValue());
        response.setContent("历史消息");
        response.setOnlineUsers(null);
        response.setHistoryMessage(historyMessages);

        session.sendMessage(new TextMessage(serializeMessage(response)));

    }

    /**
     * 发送更多历史消息给新连接用户
     */
    public void sendMoreHistoryMessages(WebSocketSession session, Long chatId) throws IOException {
        // 获取历史消息（这里假设获取最近20条）
        Page<ChatMessageVO> historyMessages = chatMessageService.getHistoryMessages(chatId, 1L, 50L);
        //处理历史消息
        ChatResponseMessage response = new ChatResponseMessage();
        response.setType(ChatMessageTypeEnum.HISTORY.getValue());
        response.setContent("历史消息");
        response.setOnlineUsers(null);
        response.setHistoryMessage(historyMessages);

        session.sendMessage(new TextMessage(serializeMessage(response)));
    }

    /**
     * 广播在线用户列表
     */
    public void broadcastOnlineUsers(Long chatId) throws IOException {
        //得到房间内的所有用户id
        Set<Long> userIds = userSession.get(chatId);
        if (CollUtil.isEmpty(userIds)) {
            return;
        }

        // 获取在线用户信息
        List<User> users = userService.listByIds(userIds);
        List<UserVO> onlineUserVos = userService.getListUserVo(users);

        ChatResponseMessage response = new ChatResponseMessage();
        response.setType(ChatMessageTypeEnum.ONLINEUSER.getValue());
        response.setOnlineUsers(onlineUserVos);
        response.setTimestamp(new Date());

        broadcastToRoom(chatId, response, null);
    }


    /**
     * 处理聊天消息（由Disruptor调用）
     */
    public void handleChatMessage(ChatRequestMessage request, WebSocketSession session, User user, Long chatId) throws IOException {
        // 保存消息到数据库
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setReplayId(request.getReplayId());
        Long userId = user.getId();
        chatMessage.setSendId(userId);
        Long receiverId = request.getReceiverId();
        chatMessage.setReceiveId(receiverId);
        chatMessage.setContent(request.getContent());
        if(request.getMessageType()!=null){
            chatMessage.setMessageType(request.getMessageType());
            chatMessage.setTargetId(request.getTargetId());
        }
        //设置会话ID
        String sessionId;
        if(userId<receiverId){
            sessionId=String.format("user%d_user%d", userId, receiverId);
        }else{
            sessionId=String.format("user%d_user%d", receiverId, userId);
        }
        chatMessage.setChatPromptId(chatId);
        chatMessage.setSessionId(sessionId);
        boolean save = chatMessageService.save(chatMessage);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR,"消息发送失败");

        //更新聊天室的信息
        ChatPrompt chatPrompt = new ChatPrompt();
        chatPrompt.setId(chatId);
        chatPrompt.setLastMessage(request.getContent());
        chatPrompt.setLastMessageTime(new Date());
        boolean update = chatPromptService.updateById(chatPrompt);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR,"聊天室消息更新失败失败");
        //todo 未读消息数修改

        //发送历史消息
        Page<ChatMessageVO> historyMessages = chatMessageService.getHistoryMessages(chatId, 1L, 20L);
        ChatResponseMessage chatResponseMessage = new ChatResponseMessage();
        chatResponseMessage.setId(chatMessage.getId());
        chatResponseMessage.setType(ChatMessageTypeEnum.SEND.getValue());
        chatResponseMessage.setContent(chatMessage.getContent());
        chatResponseMessage.setTimestamp(chatMessage.getCreateTime());
        chatResponseMessage.setUser(userService.getUserVo(user));
        chatResponseMessage.setHistoryMessage(historyMessages);

        // 广播消息给房间内其他用户
        broadcastToRoom(chatId, chatResponseMessage, session);
    }


    /**
     * 广播消息到房间（可排除特定会话）
     */
    private void broadcastToRoom(Long chatId, ChatResponseMessage message, WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> sessions = roomSessions.get(chatId);
        if (CollUtil.isEmpty(sessions)) {
            return;
        }

        String jsonMessage = serializeMessage(message);
        TextMessage textMessage = new TextMessage(jsonMessage);

        for (WebSocketSession session : sessions) {
            // 排除指定会话
//            if (excludeSession != null && excludeSession.equals(session)) {
//                continue;
//            }
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }


    /**
     * 序列化消息（处理Long类型精度问题）
     */
    private String serializeMessage(ChatResponseMessage message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module);

        return objectMapper.writeValueAsString(message);
    }


    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // 处理传输错误
        exception.printStackTrace();
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}
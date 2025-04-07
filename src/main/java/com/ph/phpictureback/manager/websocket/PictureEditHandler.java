package com.ph.phpictureback.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ph.phpictureback.manager.websocket.disruptor.PictureEditEventProducer;
import com.ph.phpictureback.manager.websocket.model.PictureEditActionEnum;
import com.ph.phpictureback.manager.websocket.model.PictureEditMessageTypeEnum;
import com.ph.phpictureback.manager.websocket.model.PictureEditRequestMessage;
import com.ph.phpictureback.manager.websocket.model.PictureEditResponseMessage;
import com.ph.phpictureback.model.entry.User;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * websocket发送消息
 */
@Component
public class PictureEditHandler extends TextWebSocketHandler {
    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;


    //key：当前编辑图片的id，value：当前编辑用户的id
    private final Map<Long, Long> pictureEditUser = new ConcurrentHashMap<>();
    //保存当前连接的所有会话，key：图片id，value：当前连接的的用户会话
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();


    /**
     * 连接成功之后发送消息
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //保存会话集合
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        //构造响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String userName = String.format("%s 加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(userName);
        pictureEditResponseMessage.setUser(userService.getUserVo(user));
        //广播用户
        this.broadcastToPicture(pictureId, pictureEditResponseMessage);
    }


    /**
     * 接收到消息之后根据不同的消息类别执行不同的处理
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        //消息转化
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        //生产消息
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);

        //使用Disruptor执行，实现异步化和并发
        /*//消息转化
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        String type = pictureEditRequestMessage.getType();
        //得到类型的枚举类
        PictureEditMessageTypeEnum enumByValue = PictureEditMessageTypeEnum.getEnumByValue(type);
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");

        //消息类型的枚举
        switch (enumByValue) {
            case ENTER_EDIT:
                handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                pictureEditResponseMessage.setUser(userService.getUserVo(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));

        }*/
    }

    /**
     * 关闭会话
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        //移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        //只有当前的会话不为空时，才删除会话
        if (sessionSet != null) {
            sessionSet.remove(session);
            //删除之后，会话为空了，直接删除当前用户的连接会话
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String format = String.format("%s离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(format);
        pictureEditResponseMessage.setUser(userService.getUserVo(user));
        //广播
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 用户正在编辑
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        Long editUserId = pictureEditUser.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        //获取到编辑的操作
        PictureEditActionEnum enumByValue = PictureEditActionEnum.getEnumByValue(editAction);
        //没有编辑操作时，直接退出
        if (enumByValue == null) {
            return;
        }
        //必须是当前用户
        if (editUserId != null && editUserId.equals(user.getId())) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String format = String.format("%s 执行了 %s", user.getUserName(), enumByValue.getText());
            pictureEditResponseMessage.setMessage(format);
            pictureEditResponseMessage.setUser(userService.getUserVo(user));
            pictureEditResponseMessage.setEditAction(editAction);
            //广播,不同步自己
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * 离开编辑状态
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        Long editPictureUserId = pictureEditUser.get(pictureId);
        //当前编辑用户不能为空，并且用户的id需要是一样的
        if (editPictureUserId != null && editPictureUserId.equals(user.getId())) {
            //移除当前用户的编辑状态
            pictureEditUser.remove(pictureId);
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String format = String.format("%s 离开编辑", user.getUserName());
            pictureEditResponseMessage.setMessage(format);
            pictureEditResponseMessage.setUser(userService.getUserVo(user));
            //广播
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 进入编辑状态
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     * @throws IOException
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {
        //没有用户编辑时才可以进入编辑状态
        if (!pictureEditUser.containsKey(pictureId)) {
            //设置响应消息
            pictureEditUser.put(pictureId, user.getId());
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String format = String.format("%s 进入编辑", user.getUserName());
            pictureEditResponseMessage.setMessage(format);
            pictureEditResponseMessage.setUser(userService.getUserVo(user));
            //广播给所有人
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 广播会话,( 排除掉当前 session 不发送 ）
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws IOException {
        //获取当前图片的会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            //创建objectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            //配置序列化，解决精度丢失的问题
            SimpleModule simpleModule = new SimpleModule();
            simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
            simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance); //支持long类型
            objectMapper.registerModule(simpleModule);
            //序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            //构建消息请求
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                //排除掉当前 session 不发送
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }
                //发送消息
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 广播会话 广播给所有人
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        this.broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }
}

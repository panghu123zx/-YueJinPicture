package com.ph.phpictureback.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import com.ph.phpictureback.manager.websocket.PictureEditHandler;
import com.ph.phpictureback.manager.websocket.model.PictureEditMessageTypeEnum;
import com.ph.phpictureback.manager.websocket.model.PictureEditRequestMessage;
import com.ph.phpictureback.manager.websocket.model.PictureEditResponseMessage;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.security.PrivateKey;
import java.util.Map;

/**
 * 事件的消费者
 */

@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserService userService;


    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        //消息转化
        PictureEditRequestMessage pictureEditRequestMessage =pictureEditEvent.getPictureEditRequestMessage();

        String type = pictureEditRequestMessage.getType();
        //得到类型的枚举类
        PictureEditMessageTypeEnum enumByValue = PictureEditMessageTypeEnum.valueOf(type);
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();

        //消息类型的枚举
        switch (enumByValue) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                pictureEditResponseMessage.setUser(userService.getUserVo(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));

        }
    }
}

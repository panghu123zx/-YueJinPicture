package com.ph.phpictureback.model.vo;

import com.ph.phpictureback.model.entry.ChatMessage;
import com.ph.phpictureback.model.entry.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 脱敏后的用户，用户用户搜索他人时展示
 */
@Data
public class ChatMessageVO implements Serializable {

    private Long id;

    /**
     * 会话id，链接为user1_user2用于区别会话的,id按大小排列
     */
    private String sessionId;

    /**
     * 回复消息的id
     */
    private Long replayId;

    /**
     * 聊天发送者的id
     */
    private Long sendId;

    /**
     * 聊天接收者的id
     */
    private Long receiveId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型 0-图片，1-文件
     */
    private Integer messageType;

    /**
     * 目标的id
     */
    private Long targetId;

    /**
     * 是否已读， 0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 消息提示的id
     */
    private Long chatPromptId;

    /**
     * 创建时间
     */
    private Date createTime;
    //todo 后期有需要修改成 AudioFileVO
    private String url;

    /**
     * 包装类转对象
     *
     * @param chatMessageVO
     * @return
     */
    public static ChatMessage voToObj(ChatMessageVO chatMessageVO) {
        if (chatMessageVO == null) {
            return null;
        }
        ChatMessage chatMessage = new ChatMessage();
        BeanUtils.copyProperties(chatMessageVO, chatMessage);

        return chatMessage;
    }

    /**
     * 对象转包装类
     *
     * @param chatMessage
     * @return
     */
    public static ChatMessageVO objToVo(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return null;
        }
        ChatMessageVO chatMessageVO = new ChatMessageVO();
        BeanUtils.copyProperties(chatMessage, chatMessageVO);
        return chatMessageVO;
    }

    private static final long serialVersionUID = 1L;
}

package com.ph.phpictureback.model.vo;

import com.ph.phpictureback.model.entry.ChatPrompt;
import com.ph.phpictureback.model.entry.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 脱敏后的用户，用户用户搜索他人时展示
 */
@Data
public class ChatPromptVO implements Serializable {

    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 目标id
     */
    private Long targetId;

    /**
     * 聊天记录的名称
     */
    private String title;

    /**
     * 对方定义的聊天名称
     */
    private String receiveTitle;

    /**
     * 聊天类型 0-私信 ,1-好友，2-群聊，3-普通
     */
    private Integer chatType;

    /**
     * 未读消息数量
     */
    private Integer unreadCount;

    /**
     * 最后一条消息内容
     */
    private String lastMessage;

    /**
     * 最后交流的时间
     */
    private Date lastMessageTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;


    /**
     * 目标用户的id
     */
    private UserVO targetUserVO;

    /**
     * 是否反向，0-正向，1-反向，指定当前列对于登入用户的方向
     */
    private Integer isReceive=0;

    /**
     * 包装类转对象
     *
     * @param chatPromptVO
     * @return
     */
    public static ChatPrompt voToObj(ChatPromptVO chatPromptVO) {
        if (chatPromptVO == null) {
            return null;
        }
        ChatPrompt chatPrompt = new ChatPrompt();
        BeanUtils.copyProperties(chatPromptVO, chatPrompt);
        return chatPrompt;
    }

    /**
     * 对象转包装类
     *
     * @param chatPrompt
     * @return
     */
    public static ChatPromptVO objToVo(ChatPrompt chatPrompt) {
        if (chatPrompt == null) {
            return null;
        }
        ChatPromptVO chatPromptVO = new ChatPromptVO();
        BeanUtils.copyProperties(chatPrompt, chatPromptVO);
        return chatPromptVO;
    }

    private static final long serialVersionUID = 1L;
}

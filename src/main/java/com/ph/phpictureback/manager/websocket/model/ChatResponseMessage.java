package com.ph.phpictureback.manager.websocket.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.model.entry.ChatMessage;
import com.ph.phpictureback.model.vo.ChatMessageVO;
import com.ph.phpictureback.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseMessage {

    private Long id; // 消息ID
    private String content; // 消息内容
    private String type; // 消息类型
    private UserVO user; // 发送用户
    private Date timestamp; // 发送时间
    private List<UserVO> onlineUsers; // 在线用户列表（仅用于ONLINE_USER类型）
    private Page<ChatMessageVO> historyMessage;  //历史消息
}

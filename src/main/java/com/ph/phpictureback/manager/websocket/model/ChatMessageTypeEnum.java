package com.ph.phpictureback.manager.websocket.model;

import lombok.Getter;

@Getter
public enum ChatMessageTypeEnum {
    SEND("发送消息", "SEND"),
    JOIN("加入聊天", "JOIN"),
    EXIT("离开聊天", "EXIT"),
    HISTORY("历史消息", "HISTORY"),
    MOREHISTORY("更多历史消息", "MOREHISTORY"),
    ERROR("错误", "ERROR"),
    ONLINEUSER("在线用户", "ONLINEUSER");



    private final String text;
    private final String value;

    ChatMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static ChatMessageTypeEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (ChatMessageTypeEnum typeEnum : ChatMessageTypeEnum.values()) {
            if (typeEnum.value.equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}
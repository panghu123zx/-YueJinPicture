package com.ph.phpictureback.model.dto.likeMessage;

import lombok.Data;

@Data
public class LikeMessageAddDto {

    /**
     * 消息接收者id
     */
    private Long receiverId;


    /**
     * 目标的类型 0-图片 1-帖子
     */
    private Integer targetType;

    /**
     * 0-点赞，1-分享，2-评论
     */
    private Integer actionType;

    /**
     * 目标的id
     */
    private Long targetId;

}

package com.ph.phpictureback.model.dto.likeMessage;

import com.ph.phpictureback.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LikeMessageQueryDto  extends PageRequest {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 消息发送者id
     */
    private Long sendId;

    /**
     * 消息接收者id
     */
    private Long receiverId;

    /**
     * 是否已读， 0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 0-点赞，1-分享
     */
    private Integer actionType;

    /**
     * 目标的类型 0-图片 1-帖子
     */
    private Integer targetType;

    /**
     * 目标的id
     */
    private Long targetId;
}

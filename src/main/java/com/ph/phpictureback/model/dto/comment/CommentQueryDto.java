package com.ph.phpictureback.model.dto.comment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ph.phpictureback.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommentQueryDto extends PageRequest implements Serializable {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 目标id
     */
    private Long targetId;

    /**
     * 目标的类型 0-图片 1-帖子
     */
    private Integer targetType;

    /**
     * 用户id
     */
    private Long userId;


    /**
     * 评论内容
     */
    private String content;

    /**
     * 父级评论id
     */
    private Long parentId;

    /**
     * 点赞数量
     */
    private Integer likeCount;

    /**
     * 回复记录id
     */
    private Long fromId;
    /**
     * 目标对象的用户id
     */
    private Long targetUserId;

    /**
     * 是否已读 0-未读 1-已读
     */
    private Integer isRead;

    /**
     * 是否是评论我的消息： 0-评论我的消息，1-我发出的评论
     */
    private Integer isCommentMy=0;




    private static final long serialVersionUID = 1L;
}

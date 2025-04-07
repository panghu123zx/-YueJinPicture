package com.ph.phpictureback.model.dto.comment;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class AddCommentDto implements Serializable {


    /**
     * 目标id
     */
    private Long targetId;

    /**
     * 评论类型 1-图片 2-帖子
     */
    private Integer targetType;

    /**
     * 评论内容
     */
    private String content;
    /**
     * 回复人名称
     */
    private String fromName;

    /**
     * 回复人id
     */
    private Long fromId;


    /**
     * 父级评论id，默认是顶级评论
     */
    private Long parentId = -1L;


    private static final long serialVersionUID = 1L;
}
package com.ph.phpictureback.model.dto.forum;

import com.ph.phpictureback.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class ForumQueryDto extends PageRequest {
    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 创建人
     */
    private Long userId;

    /**
     * 分类
     */
    private String category;


    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 浏览数
     */
    private Integer viewCount;

    /**
     * 分享数
     */
    private Integer shareCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 审核状态 0-待审核，1-通过，2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人
     */
    private Long reviewerId;

}

package com.ph.phpictureback.model.vo;

import com.ph.phpictureback.model.entry.Forum;
import com.ph.phpictureback.model.entry.ForumFile;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;

@Data
public class ForumVO {
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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 当前用户的信息
     */
    private UserVO userVO;

    /**
     * 当前帖子的文件
     */
    private List<ForumFile> forumFile;

    /**
     * 包装类转对象
     *
     * @param forumVO
     * @return
     */
    public static Forum voToObj(ForumVO forumVO) {
        if (forumVO == null) {
            return null;
        }
        Forum forum = new Forum();
        BeanUtils.copyProperties(forumVO, forum);

        return forum;
    }

    /**
     * 对象转包装类
     *
     * @param forum
     * @return
     */
    public static ForumVO objToVo(Forum forum) {
        if (forum == null) {
            return null;
        }
        ForumVO forumVO = new ForumVO();
        BeanUtils.copyProperties(forum, forumVO);
        return forumVO;
    }
}

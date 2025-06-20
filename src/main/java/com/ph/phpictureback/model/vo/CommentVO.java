package com.ph.phpictureback.model.vo;


import com.ph.phpictureback.model.entry.Comment;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 题目视图
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class CommentVO implements Serializable {
    /**
     * 主键id
     */
    private Long id;

    /**
     * 问题id
     */
    private Long targetId;

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
     * 评论的子级评论
     */
    private List<CommentVO> commentVOChildList;


    /**
     * 包装类转对象
     *
     * @param commentVO
     * @return
     */
    public static Comment voToObj(CommentVO commentVO) {
        if (commentVO == null) {
            return null;
        }
        Comment comment = new Comment();
        BeanUtils.copyProperties(commentVO, comment);

        return comment;
    }

    /**
     * 对象转包装类
     *
     * @param comment
     * @return
     */
    public static CommentVO objToVo(Comment comment) {
        if (comment == null) {
            return null;
        }
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(comment, commentVO);
        return commentVO;
    }


}

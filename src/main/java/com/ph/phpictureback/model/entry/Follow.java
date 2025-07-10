package com.ph.phpictureback.model.entry;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 关注表
 * @TableName follow
 */
@TableName(value ="follow")
@Data
public class Follow implements Serializable {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被关注的人的id
     */
    private Long userId;

    /**
     * 粉丝id
     */
    private Long followerId;

    /**
     * 是否双向关注， 0-否，1-是
     */
    private Integer isMutual;
    /**
     * 关注状态， 0-已关注，1-已取消关注
     */
    private  Integer followState;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 逻辑删除 1（true）已删除， 0（false）未删除
     */
    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
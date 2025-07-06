package com.ph.phpictureback.model.entry;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 论坛图片表
 * @TableName forum_file
 */
@TableName(value ="forum_file")
@Data
public class ForumFile implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 帖子 id
     */
    private Long forumId;

    /**
     * url
     */
    private String picUrl;
    /**
     * 缩略图Url
     */
    private String thumbnailUrl;

    /**
     * 图片类型 0-封面，1-文件
     */
    private Integer type;

    /**
     * 图片大小
     */
    private Long size;

    /**
     * 图片位置
     */
    private Integer position;
    /**
     * 排序
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
package com.ph.phpictureback.model.entry;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 视频文件表
 * @TableName audio_file
 */
@TableName(value ="audio_file")
@Data
public class AudioFile implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 存放地址
     */
    private String fileUrl;

    /**
     * 文件类型 0-图片，1-视频，2-音频
     */
    private Integer fileType;

    /**
     * 标题
     */
    private String title;

    /**
     * 大小
     */
    private Long size;

    /**
     * 简介
     */
    private String introduction;


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
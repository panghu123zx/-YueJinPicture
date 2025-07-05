package com.ph.phpictureback.model.dto.forumFile;

import lombok.Data;

@Data
public class ForumFileAddDto {
    /**
     * id 用于修改
     */
    private Long id;

    /**
     * 帖子 id
     */
    private Long forumId;

    /**
     * url
     */
    private String fileUrl;

    /**
     * 图片类型 0-封面，1-文件
     */
    private Integer type;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 图片位置
     */
    private Integer position;
}

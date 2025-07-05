package com.ph.phpictureback.model.dto.forumFile;

import lombok.Data;

@Data
public class ForumFileQueryDto {
    /**
     * id
     */
    private Long id;

    /**
     * 帖子 id
     */
    private Long forumId;

    /**
     * 图片类型 0-封面，1-文件
     */
    private Integer type;
    /**
     * 排序
     */
    private Integer sort;

}

package com.ph.phpictureback.model.dto.forum;

import lombok.Data;

@Data
public class ForumUpdateDto {
    /**
     * id
     */
    private  Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 分类
     */
    private String category;

    /**
     * 封面地址
     */
    private String url;
}

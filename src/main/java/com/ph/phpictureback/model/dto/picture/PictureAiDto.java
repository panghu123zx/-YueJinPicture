package com.ph.phpictureback.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureAiDto implements Serializable {

    /**
     * 图片内容
     */
    private String content;


    private String name;


    private static final long serialVersionUID = 1L;
}

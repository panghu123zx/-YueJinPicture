package com.ph.phpictureback.api.imageSearch.model;

import lombok.Data;

@Data
public class ImageSearchDto {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;
}

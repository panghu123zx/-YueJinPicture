package com.ph.phpictureback.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByColorDto implements Serializable {

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}

package com.ph.phpictureback.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByPictureDto implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}

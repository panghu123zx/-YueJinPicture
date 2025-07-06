package com.ph.phpictureback.model.dto.likeMessage;

import lombok.Data;

@Data
public class LikeMessageReadDto {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 是否已读， 0-未读，1-已读
     */
    private Integer isRead;
}

package com.ph.phpictureback.model.dto.forum;

import lombok.Data;

@Data
public class ForumReviewDto {
    /**
     * id
     */
    private Long id;

    /**
     * 审核状态 0-待审核，1-通过，2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;
}

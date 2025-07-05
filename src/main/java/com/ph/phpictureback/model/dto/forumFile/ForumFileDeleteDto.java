package com.ph.phpictureback.model.dto.forumFile;

import lombok.Data;

import java.util.List;

@Data
public class ForumFileDeleteDto {
    /**
     * id 集合
     */
    private List<Long> ids;
}

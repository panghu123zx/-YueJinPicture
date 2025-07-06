package com.ph.phpictureback.model.dto.comment;

import com.ph.phpictureback.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommentQueryDto extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long targetId;

    /**
     * 评论类型
     */
    private Integer targetType;

    /**
     * 是否已读 0-未读 1-已读
     */
    private Integer isRead;


    private static final long serialVersionUID = 1L;
}

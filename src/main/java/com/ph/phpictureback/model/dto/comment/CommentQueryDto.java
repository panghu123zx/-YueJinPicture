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


    private static final long serialVersionUID = 1L;
}

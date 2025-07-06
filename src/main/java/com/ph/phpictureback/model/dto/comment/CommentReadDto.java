package com.ph.phpictureback.model.dto.comment;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class CommentReadDto implements Serializable {


    /**
     * id
     */
    private Long id;

    /**
     * 是否已读 0-未读 1-已读
     */
    private Integer isRead;


    private static final long serialVersionUID = 1L;
}
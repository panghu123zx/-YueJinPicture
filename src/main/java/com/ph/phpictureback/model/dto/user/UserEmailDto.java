package com.ph.phpictureback.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加用户
 */
@Data
public class UserEmailDto implements Serializable {

    /**
     * 邮箱
     */
    private String email;

    private static final long serialVersionUID = 1L;
}

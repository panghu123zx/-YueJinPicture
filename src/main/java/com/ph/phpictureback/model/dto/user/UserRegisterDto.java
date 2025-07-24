package com.ph.phpictureback.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterDto implements Serializable {
    /**
     * 邮箱号
     */
    private String email;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;

    /**
     * 验证码
     */
    private String code;
}

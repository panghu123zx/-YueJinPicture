package com.ph.phpictureback.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterDto implements Serializable {
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}

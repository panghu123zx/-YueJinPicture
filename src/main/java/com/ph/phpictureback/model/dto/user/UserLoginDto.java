package com.ph.phpictureback.model.dto.user;

import lombok.Data;

@Data
public class UserLoginDto {
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}

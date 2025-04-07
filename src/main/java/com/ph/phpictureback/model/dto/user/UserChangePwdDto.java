package com.ph.phpictureback.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserChangePwdDto implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 旧密码
     */
    private String oldUserPassword;


    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}

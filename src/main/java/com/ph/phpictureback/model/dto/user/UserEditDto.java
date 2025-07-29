package com.ph.phpictureback.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加用户
 */
@Data
public class UserEditDto implements Serializable {


    private Long id;

    /**
     * 用户昵称
     */
    private String userName;


    /**
     * 用户简介
     */
    private String userProfile;


    private static final long serialVersionUID = 1L;
}

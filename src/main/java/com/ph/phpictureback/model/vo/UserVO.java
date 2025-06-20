package com.ph.phpictureback.model.vo;

import com.ph.phpictureback.model.entry.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 脱敏后的用户，用户用户搜索他人时展示
 */
@Data
public class UserVO implements Serializable {

    /**
     * id
     */
    private Long id;
    
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 包装类转对象
     *
     * @param userVo
     * @return
     */
    public static User voToObj(UserVO userVo) {
        if (userVo == null) {
            return null;
        }
        User user = new User();
        BeanUtils.copyProperties(userVo, user);

        return user;
    }

    /**
     * 对象转包装类
     *
     * @param user
     * @return
     */
    public static UserVO objToVo(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    private static final long serialVersionUID = 1L;
}

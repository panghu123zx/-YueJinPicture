package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ph.phpictureback.model.dto.user.UserChangePwdDto;
import com.ph.phpictureback.model.dto.user.UserEditDto;
import com.ph.phpictureback.model.dto.user.UserQueryDto;
import com.ph.phpictureback.model.entry.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.model.vo.LoginUserVo;
import com.ph.phpictureback.model.vo.UserVO;


import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 杨志亮
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-03-06 22:04:22
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    long userRegister(String userAccount,String userPassword,String checkPassword);


    /**
     * 登入
     * @param userAccount
     * @param userPassword
     * @return
     */
    LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登入信息
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 修改密码
     * @param userChangePwdDto
     * @param request
     * @return
     */
    Boolean changePassword(UserChangePwdDto userChangePwdDto, HttpServletRequest request);

    /**
     * 退出登入
     * @param request
     * @return
     */
    Boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的的单个用户
     * @param user
     * @return
     */
    UserVO getUserVo(User user);

    /**
     * 获取脱敏的的用户列表
     * @param userList
     * @return
     */
    List<UserVO> getListUserVo(List<User> userList);

    /**
     * 查询请求
     * @param queryDto
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryDto queryDto);

    /**
     * 修改用户信息
     * @param userEditDto
     * @return
     */
    boolean editUser(UserEditDto userEditDto);

    /**
     * 加密
     * @param userPassword
     * @return
     */
    String getEncipher(String userPassword);

    /**
     * 用户信息脱敏
     * @param user
     * @return
     */
    LoginUserVo getLoginUserVo(User user);

    /**
     * 判断是否为管理员
     * @param user
     * @return
     */
    Boolean isAdmin(User user);
}

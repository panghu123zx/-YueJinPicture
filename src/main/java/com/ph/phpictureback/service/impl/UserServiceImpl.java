package com.ph.phpictureback.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.constant.UserConstant;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.auth.StpKit;
import com.ph.phpictureback.model.dto.user.UserChangePwdDto;
import com.ph.phpictureback.model.dto.user.UserEditDto;
import com.ph.phpictureback.model.dto.user.UserQueryDto;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.UserRoleEnum;
import com.ph.phpictureback.model.vo.LoginUserVo;
import com.ph.phpictureback.model.vo.UserVO;
import com.ph.phpictureback.service.UserService;
import com.ph.phpictureback.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 杨志亮
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-03-06 22:04:22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 用户注册
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能小于4");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次参数不一致");
        }
        //2.检查是否重复
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("userAccount", userAccount);
        Long count = this.baseMapper.selectCount(qw);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "数据重复");
        //3.加密
        String encipherPassword = getEncipher(userPassword);
        //4.添加数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encipherPassword);
        user.setUserName(userAccount);
        user.setUserAvatar("https://img2.baidu.com/it/u=3887984625,2343006467&fm=253&fmt=auto&app=138&f=JPEG?w=199&h=199");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = this.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "数据库异常");
        return user.getId();
    }

    /**
     * 登入
     *
     * @param userAccount
     * @param userPassword
     * @return
     */
    @Override
    public LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.参数校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或密码为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能小于4");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8");
        }
        //密码加密对比
        String encipherPw = getEncipher(userPassword);
        //2.是否存在
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("userAccount", userAccount);
        qw.eq("userPassword", encipherPw);
        User user = this.baseMapper.selectOne(qw);
        if (user == null) {
            log.info("user login field,userAccount or password is error");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码或账号错误");
        }
        //添加登入态
        request.getSession().setAttribute(UserConstant.USER_LOGIN, user);
        //设置so-token的用户登入态
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN, user);


        return getLoginUserVo(user);
    }

    /**
     * 获取当前登入用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //数据库查询是否存在，保证一致性,避免缓存的影响
        currentUser = this.getById(currentUser.getId());
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public Boolean changePassword(UserChangePwdDto userChangePwdDto, HttpServletRequest request) {
        Long id = userChangePwdDto.getId();
        String oldUserPassword = userChangePwdDto.getOldUserPassword();
        String userPassword = userChangePwdDto.getUserPassword();
        String checkPassword = userChangePwdDto.getCheckPassword();
        User loginUser = getLoginUser(request);
        if (StrUtil.hasBlank(oldUserPassword, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (!userPassword.equals(checkPassword) || userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致或密码长度小于8");
        }
        if (oldUserPassword.equals(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新旧密码不能相同");
        }
        if (!loginUser.getId().equals(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户错误");
        }
        //密码加密对比
        String encipherPw = getEncipher(oldUserPassword);
        if (!loginUser.getUserPassword().equals(encipherPw)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        User user = new User();
        user.setId(id);
        user.setUserPassword(getEncipher(userPassword));
        boolean update = this.updateById(user);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "密码修改失败");
        //移除登入态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN);

        return true;
    }

    /**
     * 退出登入
     *
     * @param request
     * @return
     */
    @Override
    public Boolean userLogout(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        request.getSession().removeAttribute(UserConstant.USER_LOGIN);

        return true;
    }

    /**
     * 获取脱敏的的单个用户
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVo(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏的的用户列表
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getListUserVo(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVo).collect(Collectors.toList());
    }

    /**
     * 查询请求
     *
     * @param queryDto
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryDto queryDto) {
        if (queryDto == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = queryDto.getId();
        String userName = queryDto.getUserName();
        String userAccount = queryDto.getUserAccount();
        String userProfile = queryDto.getUserProfile();
        String userRole = queryDto.getUserRole();
        String sortField = queryDto.getSortField();
        String sortOrder = queryDto.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);

        return queryWrapper;
    }

    /**
     * 修改用户信息
     *
     * @param userEditDto
     * @return
     */
    @Override
    public boolean editUser(UserEditDto userEditDto) {
        Long id = userEditDto.getId();
        String userName = userEditDto.getUserName();
        String userAvatar = userEditDto.getUserAvatar();
        String userProfile = userEditDto.getUserProfile();
        User user = this.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        User editUser = new User();
        editUser.setId(id);
        editUser.setUserName(userName);
        editUser.setUserAvatar(userAvatar);
        editUser.setUserProfile(userProfile);
        boolean update = this.updateById(editUser);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "修改失败");

        return true;
    }

    /**
     * 加密
     *
     * @param userPassword
     * @return
     */
    @Override
    public String getEncipher(String userPassword) {
        final String SALT = "panghu";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 用户信息脱敏
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVo getLoginUserVo(User user) {
        LoginUserVo loginUserVo = new LoginUserVo();
        BeanUtils.copyProperties(user, loginUserVo);
        return loginUserVo;
    }

    /**
     * 判断是否为管理员
     *
     * @param user
     * @return
     */
    @Override
    public Boolean isAdmin(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }


}






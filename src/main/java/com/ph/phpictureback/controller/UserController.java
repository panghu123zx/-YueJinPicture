package com.ph.phpictureback.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.annotation.AuthCheck;
import com.ph.phpictureback.api.MailConfig;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.constant.UserConstant;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.user.*;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.LoginUserVo;
import com.ph.phpictureback.model.vo.UserVO;
import com.ph.phpictureback.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 健康检查
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    private MailConfig mailConfig;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 发送邮件
     * @param userEmailDto
     * @return
     */
    @PostMapping("/send")
    public BaseResponse<Boolean> sendMail(@RequestBody UserEmailDto userEmailDto) {
        ThrowUtils.throwIf(userEmailDto == null, ErrorCode.PARAMS_ERROR,"邮箱号不能为空");
        SecureRandom secureRandom = new SecureRandom();
        String code = String.format("%06d", secureRandom.nextInt(999999));
        String subject = "跃金图库-注册验证码";
        String content = "你好！感谢你注册 跃金图库，你的验证码是: "+code+",5分钟后失效请尽快使用";
        try {
            mailConfig.sendSimpleMail(userEmailDto.getEmail(),subject,content);
            String key = UserConstant.CODE + userEmailDto.getEmail();
            redisTemplate.opsForValue().set(key,code,5, TimeUnit.MINUTES);
            log.info(code);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"发送邮件失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 用户注册
     *
     * @param userRegisterDto
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterDto userRegisterDto) {
        if (userRegisterDto == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String email = userRegisterDto.getEmail();
        String userPassword = userRegisterDto.getUserPassword();
        String checkPassword = userRegisterDto.getCheckPassword();
        String code = userRegisterDto.getCode();
        long res = userService.userRegister(email, userPassword, checkPassword,code);
        return ResultUtils.success(res);
    }

    /**
     * 用户登入-
     *
     * @param userLoginDto
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVo> userLogin(@RequestBody UserLoginDto userLoginDto, HttpServletRequest request) {
        if (userLoginDto == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginDto.getUserAccount();
        String userPassword = userLoginDto.getUserPassword();
        LoginUserVo res = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(res);
    }

    /**
     * 获取当前登入用户的信息
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVo> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        LoginUserVo res = userService.getLoginUserVo(loginUser);
        return ResultUtils.success(res);
    }

    /**
     * 退出登入
     *
     * @param request
     * @return
     */
    @GetMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        Boolean b = userService.userLogout(request);
        return ResultUtils.success(b);
    }


    /**
     * 添加用户
     *
     * @param userAddDto
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> adduser(@RequestBody UserAddDto userAddDto) {
        if (userAddDto == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddDto, user);
        //设置默认密码
        String delaultPw = "12345678";
        String encipherPw = userService.getEncipher(delaultPw);
        user.setUserPassword(encipherPw);
        boolean save = userService.save(user);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(user.getId());
    }


    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取用户，并包装起来给普通用户看
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVo(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }


    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateDto userUpdateDto) {
        if (userUpdateDto == null || userUpdateDto.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateDto, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 修改用户信息
     * @param userEditDto
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editUser(@RequestBody UserEditDto userEditDto){
        ThrowUtils.throwIf(userEditDto == null, ErrorCode.PARAMS_ERROR);
        boolean b = userService.editUser(userEditDto);
        return ResultUtils.success(b);
    }


    /**
     * 分页获取数据
     *
     * @param userQueryDto
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> getListUserByPage(@RequestBody UserQueryDto userQueryDto) {
        ThrowUtils.throwIf(userQueryDto == null, ErrorCode.PARAMS_ERROR);
        int current = userQueryDto.getCurrent();
        int pageSize = userQueryDto.getPageSize();
        //得到分页查询的条件
        Page<User> page = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryDto));
        //新建分页，但是没有数据
        Page<UserVO> userVOPage = new Page<>(current, pageSize, page.getTotal());
        //得到封装的数据
        List<UserVO> listUserVo = userService.getListUserVo(page.getRecords());
        //添加到新建分页中
        userVOPage.setRecords(listUserVo);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 修改密码
     * @param userChangePwdDto
     * @param request
     * @return
     */
    @PostMapping("/changepwd")
    public BaseResponse<Boolean> changePwd(@RequestBody UserChangePwdDto userChangePwdDto,HttpServletRequest request){
        ThrowUtils.throwIf(userChangePwdDto==null,ErrorCode.PARAMS_ERROR,"参数错误");
        boolean b = userService.changePassword(userChangePwdDto,request);
        return ResultUtils.success(b);
    }
}

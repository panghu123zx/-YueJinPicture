package com.ph.phpictureback.aop;

import com.ph.phpictureback.annotation.AuthCheck;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.UserRoleEnum;
import com.ph.phpictureback.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;


    /**
     * 拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限检验注解
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        //通过注解得到当前需要那种权限
        String mustRole = authCheck.mustRole();
        //联系上下文得到request
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        //得到当前权限的值
        UserRoleEnum mustUserRole = UserRoleEnum.getUserRoleValue(mustRole);
        //不需要权限校验
        if (mustUserRole == null) {
            return null;
        }
        //得到当前登入用户
        User loginUser = userService.getLoginUser(request);
        //得到当前登入用户的权限
        UserRoleEnum userRoleValue = UserRoleEnum.getUserRoleValue(loginUser.getUserRole());
        //没有权限拒绝
        if (userRoleValue == null) {
            return joinPoint.proceed();
        }
        //当前需要的权限是admin 而 用户的权限不是admin 报错
        if (UserRoleEnum.ADMIN.equals(mustUserRole) && !UserRoleEnum.ADMIN.equals(userRoleValue)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //通行
        return joinPoint.proceed();
    }
}

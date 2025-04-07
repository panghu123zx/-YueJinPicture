package com.ph.phpictureback.manager.auth;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.ph.phpictureback.manager.auth.model.SpaceUserAuthConfig;
import com.ph.phpictureback.manager.auth.model.SpaceUserAuthContext;
import com.ph.phpictureback.manager.auth.model.SpaceUserPermissionConstant;
import com.ph.phpictureback.manager.auth.model.SpaceUserRole;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.SpaceUser;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.SpaceTypeEnum;
import com.ph.phpictureback.model.enums.SpaceUserEnum;
import com.ph.phpictureback.service.SpaceUserService;
import com.ph.phpictureback.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j

public class SpaceUserAuthManager {

    @Resource
    private UserService userService;
    @Resource
    private SpaceUserService spaceUserService;


    private static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    //初始化权限和角色列表
    static {
        String jsonConfig = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(jsonConfig, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     *
     * @param spaceUser 用户传递的角色权限： viewer、editor、admin
     * @return
     */
    public static List<String> getPermissionsBySpaceUser(String spaceUser) {
        if (StrUtil.isBlank(spaceUser)) {
            return new ArrayList<>();
        }
        //匹配角色
        SpaceUserRole spaceUserRole = SPACE_USER_AUTH_CONFIG.getRoles()
                .stream()
                .filter(role -> role.getKey().equals(spaceUser))
                .findFirst()
                .orElse(null);

        if (spaceUserRole == null) {
            return new ArrayList<>();
        }
        //返回权限列表
        return spaceUserRole.getPermissions();
    }


    /**
     * 获取当前用户权限列表
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsBySpaceUser(SpaceUserEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getSpaceTypeValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsBySpaceUser(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }


}

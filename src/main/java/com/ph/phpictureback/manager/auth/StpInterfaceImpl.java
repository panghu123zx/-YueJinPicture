package com.ph.phpictureback.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.auth.model.SpaceUserAuthContext;
import com.ph.phpictureback.manager.auth.model.SpaceUserPermissionConstant;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.SpaceUser;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.SpaceTypeEnum;
import com.ph.phpictureback.model.enums.SpaceUserEnum;
import com.ph.phpictureback.service.PictureService;
import com.ph.phpictureback.service.SpaceService;
import com.ph.phpictureback.service.SpaceUserService;
import com.ph.phpictureback.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

import static com.ph.phpictureback.constant.UserConstant.USER_LOGIN;
import static com.ph.phpictureback.manager.auth.StpKit.SPACE_TYPE;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private UserService userService;
    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    //获取前缀
    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        //只校验请求类型为space的
        if (!loginType.equals(StpKit.SPACE_TYPE)) {
            return new ArrayList<>();
        }
        //获取管理员权限
        List<String> adminPermissions = SpaceUserAuthManager.getPermissionsBySpaceUser(SpaceUserEnum.ADMIN.getValue());

        SpaceUserAuthContext authContextByRequest = getAuthContextByRequest();
        //上下文为空时，访问的是公共图库，返回管理员权限
        if (isAllFieldNull(authContextByRequest)) {
            return adminPermissions;
        }
        //获取当前登入用户
        User user = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN);
        //用户未登入，抛出异常
        if (user == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "未登录");
        }
        //获取用户的id，作为唯一标识
        Long userId = user.getId();
        //获取spaceUser对象，如果存在时，直接获取该角色的权限列表
        SpaceUser spaceUser = authContextByRequest.getSpaceUser();
        if (spaceUser != null) {
            return SpaceUserAuthManager.getPermissionsBySpaceUser(spaceUser.getSpaceRole());
        }
        //获取spaceUserId
        Long spaceUserId = authContextByRequest.getSpaceUserId();
        if (ObjectUtil.isNotNull(spaceUserId)) {
            spaceUser = spaceUserService.getById(spaceUserId);
            ThrowUtils.throwIf(spaceUser == null, ErrorCode.NO_AUTH_ERROR, "空间用户不存在");
            //校验当前用户是否为该空间加入者
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if(loginSpaceUser==null){
                return new ArrayList<>();
            }
            //为当前空间加入人时，返回权限列表
            return SpaceUserAuthManager.getPermissionsBySpaceUser(loginSpaceUser.getSpaceRole());
        }
        //获取spaceId
        Long spaceId = authContextByRequest.getSpaceId();
        Long pictureId = authContextByRequest.getPictureId();
        //如果空间id不存在时，通过pictureid获取spaceId
        if (ObjectUtil.isNull(spaceId)) {
            //如果pictureId不存在时，为管理员权限
            if (ObjectUtil.isNull(pictureId)) {
                return adminPermissions;
            }
            //图片的校验逻辑
            Picture picture = pictureService.getById(pictureId);
            ThrowUtils.throwIf(picture == null, ErrorCode.NO_AUTH_ERROR, "图片不存在");
            spaceId = picture.getSpaceId();
            //公共图库
            if (spaceId == null) {
                if (picture.getUserId().equals(userId) || userService.isAdmin(user)) {
                    return adminPermissions;
                } else {
                    //不是自己的图片时，只能查看
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }

        }
        //spaceId存在时，通过spaceId获取space
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NO_AUTH_ERROR, "空间不存在");
        Integer spaceType = space.getSpaceType();
        if (spaceType.equals(SpaceTypeEnum.PRIVATE.getValue())) {
            //为私人空间时，只有空间的创建人返回管理员权限，其他人返回空列表
            if (space.getUserId().equals(userId) || userService.isAdmin(user)) {
                return adminPermissions;
            } else {
                return new ArrayList<>();
            }
        } else {
            //团队空间
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            //没有该用户
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            //返回该用户对应的权限列表
            return SpaceUserAuthManager.getPermissionsBySpaceUser(spaceUser.getSpaceRole());
        }

    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * 根据请求来获取上下文
     *
     * @return
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        //获取请求头中的 Content-Type，得到请求的类型
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext spaceUserAuthContext;
        //获取请求参数
        if (ContentType.JSON.getValue().equals(contentType)) {
            //请求类型为JSON时
            String body = ServletUtil.getBody(request);
            spaceUserAuthContext = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            spaceUserAuthContext = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        //根据请求路径，确定id 的含义
        Long id = spaceUserAuthContext.getId();
        if (ObjectUtil.isNotNull(id)) {
            String requestURI = request.getRequestURI();
            //替换请求前缀，得到请求路径 /api/picture 替换api/ 得到picture
            String replace = requestURI.replace(contextPath + "/", "");
            //得到 / 分隔符钱前面的 字符串  ，false指的是 是否是最后一个字符
            String controllerName = StrUtil.subBefore(replace, "/", false);
            //根据请求的前缀判断是什么请求，从而确定 是那个请求的 id
            switch (controllerName) {
                case "space":
                    spaceUserAuthContext.setSpaceId(id);
                    break;
                case "spaceUser":
                    spaceUserAuthContext.setSpaceUserId(id);
                    break;
                case "picture":
                    spaceUserAuthContext.setPictureId(id);
                    break;
            }
        }
        return spaceUserAuthContext;
    }


    /**
     * 判断对象中是否所有字段都为空
     *
     * @param object
     * @return
     */
    private boolean isAllFieldNull(Object object) {
        if (object == null) {
            return true;  //对象本身为空
        }

        return Arrays.stream(ReflectUtil.getFields(object.getClass())) //获取对象的所有字段
                .map(field -> ReflectUtil.getFieldValue(object, field)) //获取字段对应的值
                .allMatch(ObjectUtil::isEmpty);  //判断所有字段是否都为空，只有全部都为空时，返回true
    }

}

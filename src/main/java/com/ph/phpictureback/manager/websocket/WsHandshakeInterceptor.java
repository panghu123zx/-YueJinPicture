package com.ph.phpictureback.manager.websocket;

import cn.hutool.core.util.StrUtil;
import com.ph.phpictureback.manager.auth.SpaceUserAuthManager;
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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 拦截器，权限检验
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {
    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            //获取request请求
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            //获取请求参数
            String pictureId = servletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.error("缺少图片id，拒绝握手");
                return false;
            }
            //获取登录用户
            User loginUser = userService.getLoginUser(servletRequest);
            if (loginUser == null) {
                log.error("用户未登录，拒绝握手");
                return false;
            }
            //判断用户是否有该图片的权限
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.error("图片不存在，拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceService.getById(spaceId);
                if (space == null) {
                    log.error("图片未关联空间，拒绝握手");
                    return false;
                }
                if (!Objects.equals(space.getSpaceType(), SpaceTypeEnum.TEAM.getValue())) {
                    log.error("不是团队空间，拒绝握手");
                    return false;
                }
            }
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("用户没有编辑图片权限，拒绝握手");
                return false;
            }
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId", Long.valueOf(pictureId));
        }
        return true;
    }

    @Override
    public void afterHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, Exception exception) {

    }
}

package com.ph.phpictureback.manager.websocket;

import cn.hutool.core.util.StrUtil;
import com.ph.phpictureback.manager.auth.SpaceUserAuthManager;
import com.ph.phpictureback.manager.auth.model.SpaceUserPermissionConstant;
import com.ph.phpictureback.model.entry.ChatPrompt;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.SpaceTypeEnum;
import com.ph.phpictureback.service.ChatPromptService;
import com.ph.phpictureback.service.PictureService;
import com.ph.phpictureback.service.SpaceService;
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
 * 聊天功能拦截器，权限检验
 */
@Component
@Slf4j
public class WsHandshakeChatInterceptor implements HandshakeInterceptor {
    @Resource
    private UserService userService;
    @Resource
    private ChatPromptService chatPromptService;


    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            //获取request请求
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            //获取请求参数： 聊天室 的id
            String chatId = servletRequest.getParameter("chatId");
            if(StrUtil.isBlank(chatId)){
                log.error("缺少聊天室id，拒绝握手");
                return false;
            }

            //获取登录用户
            User loginUser = userService.getLoginUser(servletRequest);
            if (loginUser == null) {
                log.error("用户未登录，拒绝握手");
                return false;
            }
            //获取聊天室
            ChatPrompt chatPrompt = chatPromptService.getById(chatId);
            if(chatPrompt==null){
                log.error("聊天室不存在，拒绝握手");
                return false;
            }
            if(!chatPrompt.getUserId().equals(loginUser.getId()) && !chatPrompt.getTargetId().equals(loginUser.getId())){
                log.error("用户不在聊天室，拒绝握手");
                return false;
            }
            //todo 判断登入用户是否在聊天室内部
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("chatId", Long.valueOf(chatId));
        }
        return true;
    }

    @Override
    public void afterHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, Exception exception) {

    }
}

package com.ph.phpictureback.controller;

import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.userlike.UserLikeAddDto;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.UserLikeVO;
import com.ph.phpictureback.service.UserLikeService;
import com.ph.phpictureback.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 健康检查
 */
@RestController
@RequestMapping("/userlike")
public class UserLikeController {
    @Resource
    private UserLikeService userLikeService;

    @Resource
    private UserService userService;

    /**
     * 获取我的点赞
     * @param request
     * @return
     */
    @GetMapping("/getMyLike")
    public BaseResponse<UserLikeVO> getMyLike(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        UserLikeVO userLikeVO = userLikeService.getMyLike(loginUser);
        return ResultUtils.success(userLikeVO);
    }

    /**
     * 点赞
     * @param userLikeAddDto
     * @param request
     * @return
     */
    @PostMapping("/addUserLike")
    public BaseResponse<Boolean> addUserLike(@RequestBody UserLikeAddDto userLikeAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(userLikeAddDto == null, ErrorCode.PARAMS_ERROR,"参数不能为空");
        User loginUser = userService.getLoginUser(request);
        Boolean result = userLikeService.addUserLike(userLikeAddDto, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 取消点赞
     * @param userLikeAddDto
     * @param request
     * @return
     */
    @PostMapping("/unUserLike")
    public BaseResponse<Boolean> unUserLike(@RequestBody UserLikeAddDto userLikeAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(userLikeAddDto == null, ErrorCode.PARAMS_ERROR,"参数不能为空");
        User loginUser = userService.getLoginUser(request);
        Boolean result = userLikeService.unPictureLise(userLikeAddDto, loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 分享
     * @param userLikeAddDto
     * @param request
     * @return
     */
    @PostMapping("/addUserShare")
    public BaseResponse<Boolean> addUserShare(@RequestBody UserLikeAddDto userLikeAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(userLikeAddDto == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        User loginUser = userService.getLoginUser(request);
        Boolean result = userLikeService.addUserShare(userLikeAddDto, loginUser);
        return ResultUtils.success(result);
    }
    /**
     * 获取我的分享
     * @param request
     * @return
     */
    @GetMapping("/getMyShare")
    public BaseResponse<UserLikeVO> getMyShare(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        UserLikeVO userLikeVO = userLikeService.getMyShare(loginUser);
        return ResultUtils.success(userLikeVO);
    }
}

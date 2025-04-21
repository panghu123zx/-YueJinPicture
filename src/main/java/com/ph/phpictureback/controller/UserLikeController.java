package com.ph.phpictureback.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.PageRequest;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.constant.UserConstant;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.userlike.UserLikeAddDto;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.entry.UserLike;
import com.ph.phpictureback.model.enums.UserLikeTypeEnum;
import com.ph.phpictureback.model.vo.PictureVO;
import com.ph.phpictureback.model.vo.UserLikeVO;
import com.ph.phpictureback.service.PictureService;
import com.ph.phpictureback.service.UserLikeService;
import com.ph.phpictureback.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    @Resource
    private PictureService pictureService;

    /**
     * 获取我的点赞
     * @param request
     * @return
     */
    @GetMapping("/getMyLike")
    public BaseResponse<UserLikeVO> getMyLike(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN);
        User loginUser = (User) userObj;
        if (loginUser == null || loginUser.getId() == null) {
            return ResultUtils.success(null);
        }else{
            UserLikeVO userLikeVO = userLikeService.getMyLike(loginUser);
            return ResultUtils.success(userLikeVO);
        }
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

    /**
     * 获取我点赞的图片
     * @param pageRequest
     * @param request
     * @return
     */
    @PostMapping("/get/likepic")
    public BaseResponse<Page<Picture>> getLikePic(@RequestBody PageRequest pageRequest, HttpServletRequest request) {
        int pageSize = pageRequest.getPageSize();
        int current = pageRequest.getCurrent();

        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserLike> qw = new QueryWrapper<>();
        qw.eq("userId", loginUser.getId());
        qw.eq("likeShare", UserLikeTypeEnum.LIKE.getValue());
        UserLike userLike = userLikeService.getOne(qw);
        List<Long> picIdList = JSONUtil.toList(userLike.getLikePic(), Long.class);
        List<Picture> pictureList = pictureService.listByIds(picIdList);
        Page<Picture> page = new Page<>(current, pageSize, pictureList.size());
        page.setRecords(pictureList);
        return ResultUtils.success(page);
    }

    /**
     * 获取我分享的图片
     * @param pageRequest
     * @param request
     * @return
     */
    @PostMapping("/get/sharepic")
    public BaseResponse<Page<Picture>> getSharePic(@RequestBody PageRequest pageRequest, HttpServletRequest request) {
        int pageSize = pageRequest.getPageSize();
        int current = pageRequest.getCurrent();

        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserLike> qw = new QueryWrapper<>();
        qw.eq("userId", loginUser.getId());
        qw.eq("likeShare", UserLikeTypeEnum.SHARE.getValue());
        UserLike userLike = userLikeService.getOne(qw);
        List<Long> picIdList = JSONUtil.toList(userLike.getLikePic(), Long.class);
        List<Picture> pictureList = pictureService.listByIds(picIdList);
        Page<Picture> page = new Page<>(current, pageSize, pictureList.size());
        page.setRecords(pictureList);
        return ResultUtils.success(page);
    }

}

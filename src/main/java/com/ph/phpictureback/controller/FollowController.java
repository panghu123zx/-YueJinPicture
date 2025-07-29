package com.ph.phpictureback.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.follow.AddFollowDto;
import com.ph.phpictureback.model.dto.follow.FollowQueryDto;
import com.ph.phpictureback.model.entry.Follow;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.FollowVO;
import com.ph.phpictureback.service.FollowService;
import com.ph.phpictureback.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 关注逻辑
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private FollowService followService;
    @Resource
    private UserService userService;

    /**
     * 添加/取消关注
     * @param addFollowDto
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Boolean> addFollow(@RequestBody AddFollowDto addFollowDto, HttpServletRequest request){
        ThrowUtils.throwIf(addFollowDto==null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        boolean id = followService.addFollow(addFollowDto, loginUser);
        return ResultUtils.success(id);
    }

    /**
     * 获取关注的人
     * @param request
     * @return
     */
    @PostMapping("/get/follow")
    public BaseResponse<Page<FollowVO>> getFollowMy(@RequestBody FollowQueryDto followQueryDto, HttpServletRequest request){
        ThrowUtils.throwIf(followQueryDto==null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        Page<FollowVO> pageVO = followService.getFollowMy(followQueryDto, loginUser);
        return ResultUtils.success(pageVO);
    }

    /**
     * 查询是否关注
     * @param followQueryDto
     * @param request
     * @return
     */
    @PostMapping("/isfollow")
    public BaseResponse<Boolean> isFollow(@RequestBody FollowQueryDto followQueryDto, HttpServletRequest request){
        ThrowUtils.throwIf(followQueryDto==null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(followService.isFollow(followQueryDto, loginUser));
    }

    /**
     * 获取登入用户的关注数和粉丝数
     * @param request
     * @return
     */
    @GetMapping("/get/count")
    public BaseResponse<Follow> getFollowCount(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        Follow followCount = followService.getFollowCount(loginUser);
        return ResultUtils.success(followCount);
    }


    /**
     * 获取用户关注数和粉丝数
     * @param id
     * @return
     */
    @GetMapping("/get/usercount")
    public BaseResponse<Follow> getFollowCountByUser(long id){
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR,"用户不存在");
        Follow followCount = followService.getFollowCount(user);
        return ResultUtils.success(followCount);
    }
}

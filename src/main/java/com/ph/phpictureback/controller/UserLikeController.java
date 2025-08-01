package com.ph.phpictureback.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.PageRequest;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.constant.UserConstant;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.forum.ForumAddDto;
import com.ph.phpictureback.model.dto.picture.PictureQueryDto;
import com.ph.phpictureback.model.dto.userlike.UserLikeAddDto;
import com.ph.phpictureback.model.dto.userlike.UserLikeQueryDto;
import com.ph.phpictureback.model.entry.Forum;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.entry.UserLike;
import com.ph.phpictureback.model.enums.ForumPictureTypeEnum;
import com.ph.phpictureback.model.enums.UserLikeTypeEnum;
import com.ph.phpictureback.model.vo.ForumVO;
import com.ph.phpictureback.model.vo.PictureVO;
import com.ph.phpictureback.model.vo.UserLikeVO;
import com.ph.phpictureback.service.ForumService;
import com.ph.phpictureback.service.PictureService;
import com.ph.phpictureback.service.UserLikeService;
import com.ph.phpictureback.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 点赞系统
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

    @Resource
    private ForumService forumService;

    /**
     * 点赞
     *
     * @param userLikeAddDto
     * @param request
     * @return
     */
    @PostMapping("/addUserLike")
    public BaseResponse<Long> addUserLike(@RequestBody UserLikeAddDto userLikeAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(userLikeAddDto == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        User loginUser = userService.getLoginUser(request);
        Long result = userLikeService.addUserLike(userLikeAddDto, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 取消点赞
     *
     * @param userLikeAddDto
     * @param request
     * @return
     */
    @PostMapping("/unUserLike")
    public BaseResponse<Long> unUserLike(@RequestBody UserLikeAddDto userLikeAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(userLikeAddDto == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        User loginUser = userService.getLoginUser(request);
        Long result = userLikeService.unPictureLise(userLikeAddDto, loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 分享
     *
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
     * 获取我的点赞
     *
     * @param request
     * @return
     */
    @PostMapping("/getMyLike")
    public BaseResponse<UserLikeVO> getMyLike(@RequestBody UserLikeAddDto userLikeAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(userLikeAddDto == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        Integer targetType = userLikeAddDto.getTargetType();
        ForumPictureTypeEnum forumPictureTypeValue = ForumPictureTypeEnum.getForumPictureTypeValue(targetType);
        ThrowUtils.throwIf(forumPictureTypeValue == null, ErrorCode.PARAMS_ERROR, "参数错误");

        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN);
        User loginUser = (User) userObj;
        if (loginUser == null || loginUser.getId() == null) {
            return ResultUtils.success(null);
        } else {
            UserLike userLike = userLikeService.lambdaQuery()
                    .eq(UserLike::getUserId, loginUser.getId())
                    .eq(UserLike::getLikeShare, UserLikeTypeEnum.LIKE.getValue())
                    .eq(UserLike::getTargetType, forumPictureTypeValue.getValue())
                    .one();
            UserLikeVO userLikeVO = UserLikeVO.objToVo(userLike);

//            UserLikeVO userLikeVO = userLikeService.getMyLike(loginUser);
            return ResultUtils.success(userLikeVO);
        }
    }

    /**
     * 获取我的分享
     *
     * @param request
     * @return
     */
    @PostMapping("/getMyShare")
    public BaseResponse<UserLikeVO> getMyShare(@RequestBody UserLikeAddDto userLikeAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(userLikeAddDto == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        Integer targetType = userLikeAddDto.getTargetType();
        ForumPictureTypeEnum forumPictureTypeValue = ForumPictureTypeEnum.getForumPictureTypeValue(targetType);
        ThrowUtils.throwIf(forumPictureTypeValue == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);

        UserLike userLike = userLikeService.lambdaQuery()
                .eq(UserLike::getUserId, loginUser.getId())
                .eq(UserLike::getLikeShare, UserLikeTypeEnum.SHARE.getValue())
                .eq(UserLike::getTargetType, forumPictureTypeValue.getValue())
                .one();

        UserLikeVO userLikeVO = UserLikeVO.objToVo(userLike);
        return ResultUtils.success(userLikeVO);
    }

    /**
     * 获取我点赞/分享的图片
     *
     * @param userLikeQueryDto
     * @param request
     * @return
     */
    @PostMapping("/get/likepic")
    public BaseResponse<Page<PictureVO>> getLikePic(@RequestBody UserLikeQueryDto userLikeQueryDto
            , HttpServletRequest request) {
        int pageSize = userLikeQueryDto.getPageSize();
        int current = userLikeQueryDto.getCurrent();
        User loginUser = userService.getLoginUser(request);

        Integer targetType = userLikeQueryDto.getTargetType();
        Integer likeShare = userLikeQueryDto.getLikeShare();
        ForumPictureTypeEnum forumPictureTypeValue = ForumPictureTypeEnum.getForumPictureTypeValue(targetType);
        ThrowUtils.throwIf(forumPictureTypeValue == null, ErrorCode.PARAMS_ERROR, "参数错误");


        QueryWrapper<UserLike> qw = new QueryWrapper<>();
        qw.eq("userId", loginUser.getId());
        qw.eq("likeShare", likeShare);
        qw.eq("targetType", targetType);
        UserLike userLike = userLikeService.getOne(qw);
        if (userLike == null) {
            return ResultUtils.success(new Page<>(current, pageSize));
        }

        List<Long> picIdList = JSONUtil.toList(userLike.getLikePic(), Long.class);
        List<Picture> pictureList = pictureService.listByIds(picIdList);
        Page<Picture> page = new Page<>(current, pageSize, pictureList.size());
        pictureList = pictureList.stream()
                .skip((long) (current - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());

        page.setRecords(pictureList);
        Page<PictureVO> pictureVOPage = pictureService.listPictureVo(page, request);
        return ResultUtils.success(pictureVOPage);

    }

    /**
     * 获取我点赞/分享的帖子
     *
     * @param userLikeQueryDto
     * @param request
     * @return
     */
    @PostMapping("/get/likeforum")
    public BaseResponse<Page<ForumVO>> getLikePost(@RequestBody UserLikeQueryDto userLikeQueryDto, HttpServletRequest request) {
        int pageSize = userLikeQueryDto.getPageSize();
        int current = userLikeQueryDto.getCurrent();
        Integer targetType = userLikeQueryDto.getTargetType();
        Integer likeShare = userLikeQueryDto.getLikeShare();
        ForumPictureTypeEnum forumPictureTypeValue = ForumPictureTypeEnum.getForumPictureTypeValue(targetType);
        ThrowUtils.throwIf(forumPictureTypeValue == null, ErrorCode.PARAMS_ERROR, "参数错误");
        UserLikeTypeEnum userLikeTypeEnum = UserLikeTypeEnum.getUserLikeTypeValue(likeShare);
        ThrowUtils.throwIf(userLikeTypeEnum == null, ErrorCode.PARAMS_ERROR, "参数错误");

        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserLike> qw = new QueryWrapper<>();
        qw.eq("userId", loginUser.getId());
        qw.eq("likeShare", likeShare);
        qw.eq("targetType", forumPictureTypeValue.getValue());
        UserLike userLike = userLikeService.getOne(qw);
        if(userLike == null){
            return ResultUtils.success(new Page<>(current, pageSize));
        }
        List<Long> picIdList = JSONUtil.toList(userLike.getLikePost(), Long.class);
        List<Forum> forumList = forumService.listByIds(picIdList);
        Page<Forum> page = new Page<>(current, pageSize, forumList.size());
        forumList = forumList.stream()
                .skip((long) (current - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        page.setRecords(forumList);
        Page<ForumVO> forumVOPage = forumService.listForumVO(page);
        return ResultUtils.success(forumVOPage);
    }

}

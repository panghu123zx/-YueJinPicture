package com.ph.phpictureback.service.impl;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.ph.phpictureback.model.dto.userlike.UserLikeQueryDto;
import com.ph.phpictureback.model.entry.Forum;
import com.ph.phpictureback.model.vo.ForumVO;
import java.util.Date;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.redisCache.ForumCache;
import com.ph.phpictureback.manager.redisCache.PictureLikeCache;
import com.ph.phpictureback.manager.redisCache.PictureShareCache;
import com.ph.phpictureback.model.dto.userlike.UserLikeAddDto;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.entry.UserLike;
import com.ph.phpictureback.model.enums.ForumPictureTypeEnum;
import com.ph.phpictureback.model.enums.UserLikeTypeEnum;
import com.ph.phpictureback.model.vo.PictureVO;
import com.ph.phpictureback.model.vo.UserLikeVO;
import com.ph.phpictureback.service.ForumService;
import com.ph.phpictureback.service.PictureService;
import com.ph.phpictureback.service.UserLikeService;
import com.ph.phpictureback.mapper.UserLikeMapper;
import com.ph.phpictureback.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 杨志亮
 * @description 针对表【user_like(用户点赞表)】的数据库操作Service实现
 * @createDate 2025-04-01 16:39:18
 */
@Service
public class UserLikeServiceImpl extends ServiceImpl<UserLikeMapper, UserLike>
        implements UserLikeService {

    @Resource
    private PictureService pictureService;

    @Resource
    private PictureLikeCache pictureLikeCache;

    @Resource
    private PictureShareCache pictureShareCache;

    @Resource
    private UserService userService;
    @Resource
    private ForumCache forumCache;

    @Resource
    private ForumService forumService;

    /**
     * 点赞
     *
     * @param userLikeAddDto
     * @param loginUser
     * @return
     */
    @Override
    public Long addUserLike(UserLikeAddDto userLikeAddDto, User loginUser) {
        Long targetId = userLikeAddDto.getTargetId();
        Integer targetType = userLikeAddDto.getTargetType();
        Integer likeShare = userLikeAddDto.getLikeShare();
        UserLikeTypeEnum userLikeTypeValue = UserLikeTypeEnum.getUserLikeTypeValue(likeShare);
        ForumPictureTypeEnum forumPictureTypeValue = ForumPictureTypeEnum.getForumPictureTypeValue(targetType);
        //如果是分享时就错误
        ThrowUtils.throwIf(userLikeTypeValue==null || Objects.equals(likeShare, UserLikeTypeEnum.SHARE.getValue())
                , ErrorCode.PARAMS_ERROR, "点赞类型错误");
        //不是图片/帖子就报错
        ThrowUtils.throwIf(forumPictureTypeValue==null ,ErrorCode.PARAMS_ERROR,"类型错误");

        UserLike addUserLike = new UserLike();
        UserLike userLike = this.lambdaQuery()
                .eq(UserLike::getUserId, loginUser.getId())
                .eq(UserLike::getLikeShare,likeShare)
                .eq(UserLike::getTargetType, targetType)
                .one();

        //用户有过点赞记录
        if (userLike != null) {
            addUserLike.setId(userLike.getId());
            //判断用户是否已经点赞过
            if(targetType.equals(ForumPictureTypeEnum.PICTURE.getValue())){
                List<Long> pictureIdList = JSONUtil.toList(userLike.getLikePic(), Long.class);
                if (pictureIdList.contains(targetId)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "该图片已经点赞过，无法重复点赞");
                }
                pictureIdList.add(targetId);
                addUserLike.setLikePic(JSONUtil.toJsonStr(pictureIdList));
                //添加用户点赞的缓存
                pictureLikeCache.addPictureLikeCache(targetId);
            }else{
                List<Long> forumIdList = JSONUtil.toList(userLike.getLikePost(), Long.class);
                if (forumIdList.contains(targetId))
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "该帖子已经点赞过，无法重复点赞");
                forumIdList.add(targetId);
                addUserLike.setLikePost(JSONUtil.toJsonStr(forumIdList));
                //添加用户点赞的缓存
                forumCache.addForumLikeCache(targetId);
            }
        } else {
            addUserLike.setTargetType(targetType);
            addUserLike.setUserId(loginUser.getId());
            addUserLike.setUserName(loginUser.getUserName());
            addUserLike.setUserAvatar(loginUser.getUserAvatar());
            if(targetType.equals(ForumPictureTypeEnum.PICTURE.getValue())){
                List<Long> picIdList = new ArrayList<>();
                picIdList.add(targetId);
                addUserLike.setLikePic(JSONUtil.toJsonStr(picIdList));
                pictureLikeCache.addPictureLikeCache(targetId);
            }else{
                List<Long> forumIdList = new ArrayList<>();
                forumIdList.add(targetId);
                addUserLike.setLikePost(JSONUtil.toJsonStr(forumIdList));
                forumCache.addForumLikeCache(targetId);
            }
        }
        boolean save = this.saveOrUpdate(addUserLike);
        ThrowUtils.throwIf(!save, ErrorCode.PARAMS_ERROR, "点赞失败");
        return addUserLike.getId();

    }

    /**
     * 取消点赞
     * @param userLikeAddDto
     * @param loginUser
     * @return
     */
    public Long unPictureLise(UserLikeAddDto userLikeAddDto, User loginUser){
        Long targetId = userLikeAddDto.getTargetId();
        Integer targetType = userLikeAddDto.getTargetType();
        Integer likeShare = userLikeAddDto.getLikeShare();
        UserLikeTypeEnum userLikeTypeValue = UserLikeTypeEnum.getUserLikeTypeValue(likeShare);
        ForumPictureTypeEnum forumPictureTypeValue = ForumPictureTypeEnum.getForumPictureTypeValue(targetType);
        //取消的要是图片的点赞
        ThrowUtils.throwIf(userLikeTypeValue==null || Objects.equals(likeShare, UserLikeTypeEnum.SHARE.getValue())
                , ErrorCode.PARAMS_ERROR, "点赞类型错误");
        //点赞的要求是图片和帖子
        ThrowUtils.throwIf(forumPictureTypeValue==null ,ErrorCode.PARAMS_ERROR,"类型错误");

        UserLike userLike = this.lambdaQuery()
                .eq(UserLike::getUserId, loginUser.getId())
                .eq(UserLike::getLikeShare,likeShare)
                .eq(UserLike::getTargetType, targetType)
                .one();
        ThrowUtils.throwIf(userLike == null, ErrorCode.PARAMS_ERROR, "该用户没有点赞记录，无法取消点赞");
        if(targetType.equals(ForumPictureTypeEnum.PICTURE.getValue())){
            List<Long> pictureIdList = JSONUtil.toList(userLike.getLikePic(), Long.class);
            if (!pictureIdList.contains(targetId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该图片没有点赞过，无法取消点赞");
            }
            pictureIdList.remove(targetId);
            userLike.setLikePic(JSONUtil.toJsonStr(pictureIdList));
            //添加取消点赞的缓存
            pictureLikeCache.deletePictureLikeCache(targetId);
        }else{
            List<Long> forumIdList = JSONUtil.toList(userLike.getLikePost(), Long.class);
            if (!forumIdList.contains(targetId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该图片没有点赞过，无法取消点赞");
            }
            forumIdList.remove(targetId);
            userLike.setLikePost(JSONUtil.toJsonStr(forumIdList));
            forumCache.deleteForumLikeCache(targetId);
        }
        boolean update = this.updateById(userLike);
        ThrowUtils.throwIf(!update, ErrorCode.PARAMS_ERROR, "取消点赞失败");
        return userLike.getId();
    }


    /**
     * 分享
     *
     * @param userLikeAddDto
     * @param loginUser
     * @return
     */
    @Override
    public boolean addUserShare(UserLikeAddDto userLikeAddDto, User loginUser) {
        Long targetId = userLikeAddDto.getTargetId();
        Integer targetType = userLikeAddDto.getTargetType();
        Integer likeShare = userLikeAddDto.getLikeShare();
        UserLikeTypeEnum userLikeTypeValue = UserLikeTypeEnum.getUserLikeTypeValue(likeShare);
        ForumPictureTypeEnum forumPictureTypeValue = ForumPictureTypeEnum.getForumPictureTypeValue(targetType);
        ThrowUtils.throwIf(userLikeTypeValue==null || Objects.equals(likeShare, UserLikeTypeEnum.LIKE.getValue())
                , ErrorCode.PARAMS_ERROR, "分享类型错误");
        ThrowUtils.throwIf(forumPictureTypeValue==null ,ErrorCode.PARAMS_ERROR,"类型错误");

        UserLike addUserLike = new UserLike();
        addUserLike.setUserId(loginUser.getId());
        UserLike userLike = this.lambdaQuery()
                .eq(UserLike::getUserId, loginUser.getId())
                .eq(UserLike::getLikeShare,likeShare)
                .eq(UserLike::getTargetType, targetType)
                .one();

        //用户有过分享记录
        if (userLike != null) {
            addUserLike.setId(userLike.getId());
            if(targetType.equals(ForumPictureTypeEnum.PICTURE.getValue())){
                //判断用户是否已经分享过
                List<Long> pictureIdList = JSONUtil.toList(userLike.getLikePic(), Long.class);
                if (pictureIdList.contains(targetId)) {
                    pictureShareCache.addPictureShareCache(targetId);
                    return true;
                }
                pictureIdList.add(targetId);
                addUserLike.setLikePic(JSONUtil.toJsonStr(pictureIdList));
            }else{
                List<Long> forumIdList = JSONUtil.toList(userLike.getLikePost(), Long.class);
                if (forumIdList.contains(targetId)) {
                    forumCache.addForumShareCache(targetId);
                    return true;
                }
                forumIdList.add(targetId);
                addUserLike.setLikePost(JSONUtil.toJsonStr(forumIdList));
            }
        } else {
            addUserLike.setLikeShare(likeShare);
            addUserLike.setTargetType(targetType);
            addUserLike.setUserId(loginUser.getId());
            addUserLike.setUserName(loginUser.getUserName());
            addUserLike.setUserAvatar(loginUser.getUserAvatar());

            if(targetType.equals(ForumPictureTypeEnum.PICTURE.getValue())){
                List<Long> picIdList = new ArrayList<>();
                picIdList.add(targetId);
                addUserLike.setLikePic(JSONUtil.toJsonStr(picIdList));
                pictureShareCache.addPictureShareCache(targetId);
            }else{
                List<Long> forumIdList = new ArrayList<>();
                forumIdList.add(targetId);
                addUserLike.setLikePost(JSONUtil.toJsonStr(forumIdList));
                forumCache.addForumShareCache(targetId);
            }
        }

        boolean update = this.saveOrUpdate(addUserLike);
        ThrowUtils.throwIf(!update, ErrorCode.PARAMS_ERROR, "分享失败");
        return true;

    }

    @Override
    public QueryWrapper<UserLike> getQueryWrapper(UserLikeQueryDto userLikeQueryDto) {
        ThrowUtils.throwIf(userLikeQueryDto == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        QueryWrapper<UserLike> qw = new QueryWrapper<>();

        Long targetId = userLikeQueryDto.getTargetId();
        Integer targetType = userLikeQueryDto.getTargetType();
        Integer likeShare = userLikeQueryDto.getLikeShare();
        String sortField = userLikeQueryDto.getSortField();
        String sortOrder = userLikeQueryDto.getSortOrder();

        qw.eq(ObjectUtil.isNotNull(targetId), "targetId", targetId);
        qw.eq(ObjectUtil.isNotNull(targetType), "targetType", targetType);
        qw.eq(ObjectUtil.isNotNull(likeShare), "likeShare", likeShare);
        qw.orderBy(StrUtil.isNotEmpty(sortField),sortOrder.equals("ascend"),sortField);


        return qw;
    }



}





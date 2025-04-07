package com.ph.phpictureback.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.redisCache.PictureLikeCache;
import com.ph.phpictureback.manager.redisCache.PictureShareCache;
import com.ph.phpictureback.model.dto.userlike.UserLikeAddDto;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.entry.UserLike;
import com.ph.phpictureback.model.enums.UserLikeTypeEnum;
import com.ph.phpictureback.model.vo.PictureVO;
import com.ph.phpictureback.model.vo.UserLikeVO;
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

    /**
     * 点赞
     *
     * @param userLikeAddDto
     * @param loginUser
     * @return
     */
    @Override
    public boolean addUserLike(UserLikeAddDto userLikeAddDto, User loginUser) {
        Long targetId = userLikeAddDto.getTargetId();
        Integer targetType = userLikeAddDto.getTargetType();
        Integer likeShare = userLikeAddDto.getLikeShare();
        UserLikeTypeEnum userLikeTypeValue = UserLikeTypeEnum.getUserLikeTypeValue(likeShare);
        ThrowUtils.throwIf(userLikeTypeValue==null || Objects.equals(likeShare, UserLikeTypeEnum.SHARE.getValue()), ErrorCode.PARAMS_ERROR, "点赞类型错误");

        UserLike addUserLike = new UserLike();
        UserLike userLike = this.lambdaQuery()
                .eq(UserLike::getUserId, loginUser.getId())
                .eq(UserLike::getLikeShare,likeShare)
                .one();

        //用户有过点赞记录
        if (userLike != null) {
            //判断用户是否已经点赞过
            List<Long> pictureIdList = JSONUtil.toList(userLike.getLikePic(), Long.class);
            if (pictureIdList.contains(targetId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该图片已经点赞过，无法重复点赞");
            }
            pictureIdList.add(targetId);
            addUserLike.setId(userLike.getId());
            addUserLike.setLikePic(JSONUtil.toJsonStr(pictureIdList));
            boolean update = this.updateById(addUserLike);
            ThrowUtils.throwIf(!update, ErrorCode.PARAMS_ERROR, "点赞失败");
            //添加用户点赞的缓存
            pictureLikeCache.addPictureLikeCache(targetId);
            return true;
        } else {
            addUserLike.setTargetType(targetType);
            addUserLike.setUserId(loginUser.getId());
            addUserLike.setUserName(loginUser.getUserName());
            addUserLike.setUserAvatar(loginUser.getUserAvatar());
            List<Long> picIdList = new ArrayList<>();
            picIdList.add(targetId);
            addUserLike.setLikePic(JSONUtil.toJsonStr(picIdList));
            boolean save = this.save(addUserLike);
            ThrowUtils.throwIf(!save, ErrorCode.PARAMS_ERROR, "点赞失败");
            //添加用户点赞的缓存
            pictureLikeCache.addPictureLikeCache(targetId);
            return true;
        }

    }

    /**
     * 取消点赞
     * @param userLikeAddDto
     * @param loginUser
     * @return
     */
    public boolean unPictureLise(UserLikeAddDto userLikeAddDto, User loginUser){
        Long targetId = userLikeAddDto.getTargetId();
        Integer targetType = userLikeAddDto.getTargetType();
        Integer likeShare = userLikeAddDto.getLikeShare();
        UserLikeTypeEnum userLikeTypeValue = UserLikeTypeEnum.getUserLikeTypeValue(likeShare);
        ThrowUtils.throwIf(userLikeTypeValue==null || Objects.equals(likeShare, UserLikeTypeEnum.SHARE.getValue()), ErrorCode.PARAMS_ERROR, "点赞类型错误");

        UserLike userLike = this.lambdaQuery()
                .eq(UserLike::getUserId, loginUser.getId())
                .eq(UserLike::getLikeShare,likeShare)
                .one();
        ThrowUtils.throwIf(userLike == null, ErrorCode.PARAMS_ERROR, "该用户没有点赞记录，无法取消点赞");
        List<Long> pictureIdList = JSONUtil.toList(userLike.getLikePic(), Long.class);
        if (!pictureIdList.contains(targetId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该图片没有点赞过，无法取消点赞");
        }
        pictureIdList.remove(targetId);
        userLike.setLikePic(JSONUtil.toJsonStr(pictureIdList));
        boolean update = this.updateById(userLike);
        ThrowUtils.throwIf(!update, ErrorCode.PARAMS_ERROR, "取消点赞失败");
        //添加用户点赞的缓存
        pictureLikeCache.deletePictureLikeCache(targetId);
        return true;
    }

    /**
     * 获取我的点赞
     * @param loginUser
     * @return
     */
    @Override
    public UserLikeVO getMyLike(User loginUser) {
        UserLike userLike = this.lambdaQuery()
                .eq(UserLike::getUserId, loginUser.getId())
                .eq(UserLike::getLikeShare, UserLikeTypeEnum.LIKE.getValue())
                .one();
        if(ObjectUtil.isNull(userLike) || ObjectUtil.isEmpty(userLike.getLikePic())){
            return new UserLikeVO();
        }
        UserLikeVO userLikeVO = new UserLikeVO();
        BeanUtils.copyProperties(userLike, userLikeVO);
        List<Long> pictureIdList = JSONUtil.toList(userLike.getLikePic(), Long.class);
        List<Picture> pictureList = pictureService.listByIds(pictureIdList);
        //vo类型的变换
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        //获取用户信息
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());

        //让用户的id和用户一一对应，组成map集合
        Map<Long, List<User>> userIdListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        //将用户信息设置到图片中
        pictureVOList.forEach(pictureVO -> {
            //取出分页的每一个Id
            Long userId = pictureVO.getUserId();
            User user = null;
            //判断集合中是否存在
            if (userIdListMap.containsKey(userId)) {
                //根据userId查询第一个的用户
                user = userIdListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVo(user));
        });

        userLikeVO.setLikePic(pictureVOList);
        return userLikeVO;
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
        ThrowUtils.throwIf(userLikeTypeValue==null || Objects.equals(likeShare, UserLikeTypeEnum.LIKE.getValue()), ErrorCode.PARAMS_ERROR, "分享类型错误");

        UserLike addUserLike = new UserLike();
        UserLike userLike = this.lambdaQuery()
                .eq(UserLike::getUserId, loginUser.getId())
                .eq(UserLike::getLikeShare,likeShare)
                .one();

        //用户有过分享记录
        if (userLike != null) {
            //判断用户是否已经分享过
            List<Long> pictureIdList = JSONUtil.toList(userLike.getLikePic(), Long.class);
            if (pictureIdList.contains(targetId)) {
                pictureShareCache.addPictureShareCache(targetId);
                return true;
            }
            pictureIdList.add(targetId);
            addUserLike.setId(userLike.getId());
            addUserLike.setLikePic(JSONUtil.toJsonStr(pictureIdList));
            boolean update = this.updateById(addUserLike);
            ThrowUtils.throwIf(!update, ErrorCode.PARAMS_ERROR, "分享失败");
            //添加用户分享的缓存
            pictureShareCache.addPictureShareCache(targetId);
            return true;
        } else {
            addUserLike.setLikeShare(likeShare);
            addUserLike.setTargetType(targetType);
            addUserLike.setUserId(loginUser.getId());
            addUserLike.setUserName(loginUser.getUserName());
            addUserLike.setUserAvatar(loginUser.getUserAvatar());
            List<Long> picIdList = new ArrayList<>();
            picIdList.add(targetId);
            addUserLike.setLikePic(JSONUtil.toJsonStr(picIdList));
            boolean save = this.save(addUserLike);
            ThrowUtils.throwIf(!save, ErrorCode.PARAMS_ERROR, "分享失败");
            //添加用户分享的缓存
            pictureShareCache.addPictureShareCache(targetId);
            return true;
        }

    }

    /**
     * 获取我的分享
     * @param loginUser
     * @return
     */
    @Override
    public UserLikeVO getMyShare(User loginUser) {
        UserLike userLike = this.lambdaQuery()
                .eq(UserLike::getUserId, loginUser.getId())
                .eq(UserLike::getLikeShare, UserLikeTypeEnum.SHARE.getValue())
                .one();
        if(ObjectUtil.isNull(userLike) || ObjectUtil.isEmpty(userLike.getLikePic())){
            return new UserLikeVO();
        }
        UserLikeVO userLikeVO = new UserLikeVO();
        BeanUtils.copyProperties(userLike, userLikeVO);
        List<Long> pictureIdList = JSONUtil.toList(userLike.getLikePic(), Long.class);


        List<Picture> pictureList = pictureService.listByIds(pictureIdList);
        //vo类型的变换
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        //获取用户信息
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());

        //让用户的id和用户一一对应，组成map集合
        Map<Long, List<User>> userIdListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        //将用户信息设置到图片中
        pictureVOList.forEach(pictureVO -> {
            //取出分页的每一个Id
            Long userId = pictureVO.getUserId();
            User user = null;
            //判断集合中是否存在
            if (userIdListMap.containsKey(userId)) {
                //根据userId查询第一个的用户
                user = userIdListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVo(user));
        });

        userLikeVO.setLikePic(pictureVOList);
        return userLikeVO;
    }


}





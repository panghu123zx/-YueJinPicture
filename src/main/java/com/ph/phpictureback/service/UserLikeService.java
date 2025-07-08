package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.model.dto.userlike.UserLikeAddDto;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.entry.UserLike;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.model.vo.UserLikeVO;

/**
* @author 杨志亮
* @description 针对表【user_like(用户点赞表)】的数据库操作Service
* @createDate 2025-04-01 16:39:18
*/
public interface UserLikeService extends IService<UserLike> {

    /**
     * 点赞
     * @param userLikeAddDto
     * @param loginUser
     * @return
     */
    Long addUserLike(UserLikeAddDto userLikeAddDto, User loginUser);

    /**
     * 取消点赞
     * @param userLikeAddDto
     * @param loginUser
     * @return
     */
    Long unPictureLise(UserLikeAddDto userLikeAddDto, User loginUser);


    /**
     * 分享
     * @param userLikeAddDto
     * @param loginUser
     * @return
     */
    boolean addUserShare(UserLikeAddDto userLikeAddDto, User loginUser);


}

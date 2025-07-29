package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.model.dto.follow.AddFollowDto;
import com.ph.phpictureback.model.dto.follow.FollowQueryDto;
import com.ph.phpictureback.model.entry.Follow;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.FollowVO;

import java.util.List;

/**
* @author 杨志亮
* @description 针对表【follow(关注表)】的数据库操作Service
* @createDate 2025-07-09 17:23:26
*/
public interface FollowService extends IService<Follow> {

    /**
     * 添加关注
     * @param addFollowDto
     * @param loginUser
     * @return
     */
    Boolean addFollow(AddFollowDto addFollowDto, User loginUser);

    /**
     * 获取关注的人
     * @param loginUser
     */
    Page<FollowVO> getFollowMy(FollowQueryDto followQueryDto, User loginUser);




    QueryWrapper<Follow> getQueryWrapper(FollowQueryDto followQueryDto);

    /**
     * 是否关注
     * @param followQueryDto
     * @param loginUser
     * @return
     */
    Boolean isFollow(FollowQueryDto followQueryDto, User loginUser);

    /**
     * 获取关注/粉丝数
     * @param loginUser
     */
    Follow getFollowCount(User loginUser);

    /**
     * 获取我关注的人的id
     * @param loginUser
     * @return
     */
    List<Long> getListFollow(User loginUser);
}

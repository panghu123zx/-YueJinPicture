package com.ph.phpictureback.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.follow.AddFollowDto;
import com.ph.phpictureback.model.dto.follow.FollowQueryDto;
import com.ph.phpictureback.model.entry.Follow;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.FollowVO;
import com.ph.phpictureback.service.FollowService;
import com.ph.phpictureback.mapper.FollowMapper;
import com.ph.phpictureback.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 杨志亮
 * @description 针对表【follow(关注表)】的数据库操作Service实现
 * @createDate 2025-07-09 17:23:26
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow>
        implements FollowService {

    @Resource
    private UserService userService;

    /**
     * 添加关注
     *
     * @param addFollowDto
     * @param loginUser
     * @return
     */
    @Override
    public Boolean addFollow(AddFollowDto addFollowDto, User loginUser) {
        Long followerId = addFollowDto.getFollowerId();
        Long userId = addFollowDto.getUserId();
        ThrowUtils.throwIf(followerId == null || userId == null, ErrorCode.PARAMS_ERROR);
        //0:添加关注 1-取消关注
        Integer status = addFollowDto.getStatus();
        ThrowUtils.throwIf(status != 0 && status != 1, ErrorCode.PARAMS_ERROR);
        /** 逻辑拆解： isMutual 0-未互相关注 1-互相关注    followState 0-已关注 1-已取消关注
         *   0 0  表示a关注了b
         *   0 1  表示a取消关注了b
         *   1 0  表示a和b互相关注了
         *   1 1  表示b关注a，但是a取消了关注b
         */

        QueryWrapper<Follow> qw = new QueryWrapper<>();
        qw.eq("followerId", followerId).eq("userId", userId);
        Follow follow = this.getOne(qw);
        //没有我关注你的这条记录
        if (follow == null) {
            //查看反向是否有记录
            QueryWrapper<Follow> qwReceive = new QueryWrapper<>();
            qwReceive.eq("followerId", userId).eq("userId", followerId);
            Follow followReceive = this.getOne(qwReceive);
            //反向的不存在直接添加
            if (followReceive == null) {
                //添加关注
                if (status == 0) {
                    follow = new Follow();
                    follow.setFollowerId(followerId);
                    follow.setUserId(userId);
                    return this.save(follow);
                } else {//取消关注
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "没有关注记录");
                }

            }
            // 反向的存在
            Integer isMutual = followReceive.getIsMutual();
            Integer followState = followReceive.getFollowState();
            //反向的关注了我，但是我没有关注他                  对方取消关注了我
            if ((isMutual == 0 && followState == 0) || (isMutual == 0 && followState == 1)) {
                follow = new Follow();
                follow.setId(followReceive.getId());
                if (status == 0) {
                    follow.setIsMutual(1);
                } else {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "没有关注记录");
                }
                return this.updateById(follow);
            }
            //我也关注了对方
            if ((isMutual == 1 && followState == 0) || (isMutual == 1 && followState == 1)) {
                if (status == 0) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "已经关注过了");
                } else {
                    follow = new Follow();
                    follow.setId(followReceive.getId());
                    follow.setIsMutual(0);
                    return this.updateById(follow);
                }
            }
        } else {
            //有我关注了你的这条记录
            Integer followState = follow.getFollowState();
            Integer isMutual = follow.getIsMutual();
            if ((isMutual == 0 && followState == 0) || (isMutual == 1 && followState == 0)) {
                if (status == 0) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "已经关注过了");
                } else {
                    follow.setFollowState(1);
                    return this.updateById(follow);
                }

            }
            if ((isMutual == 1 && followState == 1) || (isMutual == 0 && followState == 1)) {
                if (status == 0) {
                    follow.setFollowState(0);
                    return this.updateById(follow);
                } else {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "没有关注记录");
                }

            }

        }
        return true;
    }

    /**
     * 获取关注的人
     *
     * @param loginUser
     * @param followQueryDto
     */
    @Override
    public Page<FollowVO> getFollowMy(FollowQueryDto followQueryDto, User loginUser) {
        QueryWrapper<Follow> qw = new QueryWrapper<>();
        Long id = loginUser.getId();
        Integer queryStatus = followQueryDto.getQueryStatus();
        ThrowUtils.throwIf(queryStatus != 0 && queryStatus != 1, ErrorCode.PARAMS_ERROR);
        //查询我关注的
        if (queryStatus == 0) {
            qw.and(qw1 ->
                    qw1.eq("followState", 0)
                            .eq("followerId", id)
                            .or()
                            .eq("isMutual", 1)
                            .eq("userId", id));
        } else {
            //查询关注我的
            qw.and(qw1 ->
                    qw1.eq("followState", 0)
                            .eq("userId", id)
                            .or()
                            .eq("isMutual", 1)
                            .eq("followerId", id));
        }

        //进行page页的获取，大小是固定的
        int current = followQueryDto.getCurrent();
        int pageSize = followQueryDto.getPageSize();
        //得到关注我的列
        Page<Follow> page = this.page(new Page<>(current, pageSize), qw);
        List<Follow> followList = page.getRecords();
        Page<FollowVO> pageVO = new Page<>(page.getCurrent(), page.getPages(), page.getTotal());
        if (CollUtil.isEmpty(followList)) {
            return pageVO;
        }
        //将列转成vo
        List<FollowVO> followVOList = followList.stream()
                .map(FollowVO::objToVo)
                .collect(Collectors.toList());
        //得到用户信息
        Set<Long> idSet = followList.stream().map(follow -> {
            if (follow.getUserId().equals(id)) {
                return follow.getFollowerId();
            } else {
                return follow.getUserId();
            }
        }).collect(Collectors.toSet());
        //将用户的id和列一一对应
        Map<Long, List<User>> userMap = userService.listByIds(idSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        followVOList.forEach(followVO -> {
            //粉丝
            Long followerId = followVO.getFollowerId();
            //被关注的人
            Long userId = followVO.getUserId();
            //当我作为关注的人，关注我的人应该是userid
            if (id.equals(followerId)) {
                if (userMap.containsKey(userId)) {
                    User user = userMap.get(userId).get(0);
                    followVO.setFollowerVO(userService.getUserVo(user));
                }
            } else {
                if (userMap.containsKey(followerId)) {
                    User user = userMap.get(followerId).get(0);
                    followVO.setFollowerVO(userService.getUserVo(user));
                }
            }
        });

        pageVO.setRecords(followVOList);
        return pageVO;


    }


    @Override
    public QueryWrapper<Follow> getQueryWrapper(FollowQueryDto followQueryDto) {
        ThrowUtils.throwIf(followQueryDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        QueryWrapper<Follow> qw = new QueryWrapper<>();
        Long id = followQueryDto.getId();
        Long userId = followQueryDto.getUserId();
        Long followerId = followQueryDto.getFollowerId();
        Integer isMutual = followQueryDto.getIsMutual();
        Integer followState = followQueryDto.getFollowState();
        String sortField = followQueryDto.getSortField();
        String sortOrder = followQueryDto.getSortOrder();

        qw.eq(ObjectUtil.isNotNull(id), "id", id);
        qw.eq(ObjectUtil.isNotNull(userId), "userId", userId);
        qw.eq(ObjectUtil.isNotNull(followerId), "followerId", followerId);
        qw.eq(ObjectUtil.isNotNull(isMutual), "isMutual", isMutual);
        qw.eq(ObjectUtil.isNotNull(followState), "followState", followState);


        qw.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return qw;
    }

    /**
     * 是否关注
     *
     * @param followQueryDto
     * @param loginUser
     * @return
     */
    @Override
    public Boolean isFollow(FollowQueryDto followQueryDto, User loginUser) {
        Long userId = followQueryDto.getUserId();
        Long id = loginUser.getId();
        //是我创建的这一列
        boolean existsMyFollow = this.lambdaQuery()
                .eq(Follow::getFollowerId, id)
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowState, 0)
                .exists();
        if (existsMyFollow) {
            return true;
        }
        //对方创建了这一项 我是b 他是 a a是否关注b
        return this.lambdaQuery()
                .eq(Follow::getFollowerId, userId)
                .eq(Follow::getUserId, id)
                .eq(Follow::getIsMutual, 1)
                .exists();
    }

    /**
     * 获取关注/粉丝数
     * @param loginUser
     * @return
     */
    @Override
    public Follow getFollowCount(User loginUser) {
        Long id = loginUser.getId();
        //查询我关注的
        QueryWrapper<Follow> qwMyFollow = new QueryWrapper<>();
        qwMyFollow.and(qw1 ->
                qw1.eq("followState", 0)
                        .eq("followerId", id)
                        .or()
                        .eq("isMutual", 1)
                        .eq("userId", id));
        long myFollow = this.count(qwMyFollow);
        //查询关注我的
        QueryWrapper<Follow> qwFollowMy = new QueryWrapper<>();
        qwFollowMy.and(qw1 ->
                qw1.eq("followState", 0)
                        .eq("userId", id)
                        .or()
                        .eq("isMutual", 1)
                        .eq("followerId", id));
        long followMy = this.count(qwFollowMy);
        Follow follow = new Follow();
        //直接用id存放
        //粉丝数，也就是关注我的
        follow.setFollowerId(followMy);
        //博主数，也就是我关注的
        follow.setUserId(myFollow);
        return follow;
    }

}





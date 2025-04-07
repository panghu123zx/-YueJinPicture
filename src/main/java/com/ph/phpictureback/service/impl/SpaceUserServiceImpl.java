package com.ph.phpictureback.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.space.SpaceAddDto;
import com.ph.phpictureback.model.dto.space.SpaceQueryDto;
import com.ph.phpictureback.model.dto.spaceuser.SpaceUserAddDto;
import com.ph.phpictureback.model.dto.spaceuser.SpaceUserQueryDto;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.SpaceUser;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.SpaceLevelEnum;
import com.ph.phpictureback.model.enums.SpaceTypeEnum;
import com.ph.phpictureback.model.enums.SpaceUserEnum;
import com.ph.phpictureback.model.vo.SpaceUserVO;
import com.ph.phpictureback.model.vo.SpaceVO;
import com.ph.phpictureback.model.vo.UserVO;
import com.ph.phpictureback.service.SpaceService;
import com.ph.phpictureback.service.SpaceUserService;
import com.ph.phpictureback.mapper.SpaceUserMapper;
import com.ph.phpictureback.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 杨志亮
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-03-27 16:53:57
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    @Lazy
    private SpaceService spaceService;
    @Resource
    private UserService userService;

    /**
     * 添加空间成员
     *
     * @param spaceUserAddDto
     * @return
     */
    @Override
    public Long addSpaceUser(SpaceUserAddDto spaceUserAddDto) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddDto, spaceUser);
        //检验参数
        this.validSpaceUser(spaceUser, true);
        boolean save = this.save(spaceUser);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR);
        return spaceUser.getId();
    }

    /**
     * 查询请求
     *
     * @param spaceUserQueryDto
     * @return
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryDto spaceUserQueryDto) {
        ThrowUtils.throwIf(spaceUserQueryDto == null, ErrorCode.PARAMS_ERROR);

        Long id = spaceUserQueryDto.getId();
        Long spaceId = spaceUserQueryDto.getSpaceId();
        Long userId = spaceUserQueryDto.getUserId();
        String spaceRole = spaceUserQueryDto.getSpaceRole();

        QueryWrapper<SpaceUser> qw = new QueryWrapper<>();
        qw.eq(ObjUtil.isNotNull(id), "id", id);
        qw.eq(ObjUtil.isNotNull(userId), "userId", userId);
        qw.eq(ObjUtil.isNotNull(spaceId), "spaceId", spaceId);
        qw.eq(ObjUtil.isNotNull(spaceRole), "spaceRole", spaceRole);
        return qw;
    }

    @Override
    public SpaceUserVO getUserBySpaceUser(SpaceUser spaceUser) {
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        Long userId = spaceUser.getUserId();
        if (ObjUtil.isNotNull(userId)) {
            User user = userService.getById(userId);
            UserVO userVo = userService.getUserVo(user);
            spaceUserVO.setUser(userVo);
        }
        Long spaceId = spaceUser.getSpaceId();
        if (ObjUtil.isNotNull(spaceId)) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getUserBySpace(space);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> listSpaceUserVo(List<SpaceUser> spaceUserList) {
        if (ObjUtil.isEmpty(spaceUserList)) {
            return new ArrayList<>();
        }
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());

        Set<Long> userIdList = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdList = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());

        Map<Long, List<User>> userMap = userService.listByIds(userIdList).stream().collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceMap = spaceService.listByIds(spaceIdList).stream().collect(Collectors.groupingBy(Space::getId));
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            User user = null;
            if (userMap.containsKey(userId)) {
                user = userMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVo(user));

            Long spaceId = spaceUserVO.getSpaceId();
            Space space = null;
            if (spaceMap.containsKey(spaceId)) {
                space = spaceMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(spaceService.getUserBySpace(space));
        });
        return spaceUserVOList;

    }


    /**
     * 校验空间成员信息
     *
     * @param spaceUser
     * @param add
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, Boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        String spaceRole = spaceUser.getSpaceRole();
        if (add) {
            Long spaceId = spaceUser.getSpaceId();
            Long userId = spaceUser.getUserId();
            ThrowUtils.throwIf(ObjUtil.isNull(spaceId) || ObjUtil.isNull(userId), ErrorCode.PARAMS_ERROR, "用户和空间不能为空");
            if (spaceRole == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色不能为空");
            }
            User user = userService.getById(userId);
            ThrowUtils.throwIf(ObjUtil.isNull(user), ErrorCode.PARAMS_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isNull(space), ErrorCode.PARAMS_ERROR, "空间不存在");
        }
        SpaceUserEnum userRoleValue = SpaceUserEnum.getUserRoleValue(spaceRole);
        if (spaceRole != null && userRoleValue == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色错误");
        }

    }


}





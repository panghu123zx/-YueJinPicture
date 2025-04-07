package com.ph.phpictureback.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.space.SpaceAddDto;
import com.ph.phpictureback.model.dto.space.SpaceQueryDto;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.SpaceUser;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.SpaceLevelEnum;
import com.ph.phpictureback.model.enums.SpaceTypeEnum;
import com.ph.phpictureback.model.enums.SpaceUserEnum;
import com.ph.phpictureback.model.vo.SpaceVO;
import com.ph.phpictureback.model.vo.UserVO;
import com.ph.phpictureback.service.SpaceService;
import com.ph.phpictureback.mapper.SpaceMapper;
import com.ph.phpictureback.service.SpaceUserService;
import com.ph.phpictureback.service.UserService;
import lombok.Synchronized;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author 杨志亮
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-03-19 16:58:18
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {


    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    /**
     * 创建空间
     *
     * @param spaceAddDto
     * @param loginUser
     * @return
     */
    @Override
    public Long addSpace(SpaceAddDto spaceAddDto, User loginUser) {
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddDto, space);
        //没有传值时，应该有默认值
        if (space.getSpaceName() == null) {
            space.setSpaceName("我的空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        //向空间中填充数据
        this.fillSpaceData(space);
        //检验参数
        this.validSpace(space, true);
        //权限检验
        if (!SpaceLevelEnum.COMMON.getValue().equals(space.getSpaceLevel()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "只有管理员才能创建专业版或旗舰版空间");
        }

        //设置空间创建人的信息
        Long userId = loginUser.getId();
        String lock = String.valueOf(userId).intern();
        //加锁
        synchronized (lock) {
            Long spaceId = transactionTemplate.execute(status -> {
                //一个用户只能创建一个个人空间和一个团队空间
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.SYSTEM_ERROR, "用户已存在空间");
                space.setUserId(userId);
                //保存空间信息
                boolean save = this.save(space);
                ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "保存空间信息失败");
                //创建的是团队空间时，创建人默认加入，而且是管理员
                if (SpaceTypeEnum.TEAM.getValue().equals(space.getSpaceType())) {
                    //设置用户与空间的关系
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceUserEnum.ADMIN.getValue());
                    boolean saveSpaceUser = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!saveSpaceUser, ErrorCode.SYSTEM_ERROR, "创建团队空间失败");

                }

                return space.getId();
            });
            return spaceId;
        }

    }

    /**
     * 查询请求
     *
     * @param spaceQueryDto
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryDto spaceQueryDto) {
        ThrowUtils.throwIf(spaceQueryDto == null, ErrorCode.PARAMS_ERROR);

        Long id = spaceQueryDto.getId();
        Long userId = spaceQueryDto.getUserId();
        String spaceName = spaceQueryDto.getSpaceName();
        Integer spaceLevel = spaceQueryDto.getSpaceLevel();
        String sortField = spaceQueryDto.getSortField();
        String sortOrder = spaceQueryDto.getSortOrder();
        Integer spaceType = spaceQueryDto.getSpaceType();

        QueryWrapper<Space> qw = new QueryWrapper<>();
        qw.eq(ObjUtil.isNotNull(id), "id", id);
        qw.eq(ObjUtil.isNotNull(userId), "userId", userId);
        qw.eq(ObjUtil.isNotNull(spaceType), "spaceType", spaceType);
        qw.like(ObjUtil.isNotNull(spaceName), "spaceName", spaceName);
        qw.eq(ObjUtil.isNotNull(spaceLevel), "spaceLevel", spaceLevel);
        qw.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return qw;
    }

    @Override
    public SpaceVO getUserBySpace(Space space) {
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        Long userId = space.getUserId();
        if (ObjUtil.isNotNull(userId)) {
            User user = userService.getById(userId);
            UserVO userVo = userService.getUserVo(user);
            spaceVO.setUser(userVo);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> listSpaceVo(Page<Space> page, HttpServletRequest request) {
        List<Space> spaceList = page.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        //转化为vo类型
        List<SpaceVO> spaceVoList = spaceList
                .stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        if (ObjUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        //获取用户id集合
        Set<Long> userIdList = spaceVoList.stream().map(SpaceVO::getUserId).collect(Collectors.toSet());

        Map<Long, List<User>> usermap = userService.listByIds(userIdList)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        spaceVoList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (usermap.containsKey(userId)) {
                user = usermap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVo(user));
        });
        spaceVOPage.setRecords(spaceVoList);
        return spaceVOPage;
    }

    /**
     * 检查空间权限
     *
     * @param space
     * @param loginUser
     * @return
     */
    public void checkSpaceAuth(Space space, User loginUser) {
        //只有空间的管理员和管理员可以访问空间
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }


    /**
     * 校验空间信息，只校验参数
     *
     * @param space
     * @param add
     */
    @Override
    public void validSpace(Space space, Boolean add) {
        Integer spaceLevel = space.getSpaceLevel();
        String spaceName = space.getSpaceName();
        Integer spaceType = space.getSpaceType();
        //如果是添加操作时，不能为空
        if (add) {
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级不能为空");
            }
            if (spaceName == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
        }

        SpaceLevelEnum levelEnum = SpaceLevelEnum.getSpaceLevelValue(spaceLevel);
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getSpaceTypeValue(spaceType);
        if (levelEnum == null && spaceLevel != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级错误");
        }
        if (spaceName != null && spaceName.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能超过20个字符");
        }
        if (spaceTypeEnum == null && spaceType != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型错误");
        }

    }

    /**
     * 填充空间信息
     *
     * @param space
     */
    @Override
    public void fillSpaceData(Space space) {
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum levelEnum = SpaceLevelEnum.getSpaceLevelValue(spaceLevel);
        ThrowUtils.throwIf(levelEnum == null, ErrorCode.PARAMS_ERROR, "空间等级错误");
        //只有space中数据为空时，填充数据
        if (space.getMaxSize() == null) {
            space.setMaxSize(levelEnum.getMaxSize());
        }
        if (space.getMaxCount() == null) {
            space.setMaxCount(levelEnum.getMaxCount());
        }
    }
}





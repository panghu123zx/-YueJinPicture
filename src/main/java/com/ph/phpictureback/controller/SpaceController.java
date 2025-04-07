package com.ph.phpictureback.controller;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.ph.phpictureback.annotation.AuthCheck;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.constant.UserConstant;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.auth.SpaceUserAuthManager;
import com.ph.phpictureback.model.dto.picture.PictureQueryDto;
import com.ph.phpictureback.model.dto.space.*;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.ReviewStatusEnum;
import com.ph.phpictureback.model.enums.SpaceLevelEnum;
import com.ph.phpictureback.model.vo.PictureVO;
import com.ph.phpictureback.model.vo.SpaceVO;
import com.ph.phpictureback.service.SpaceService;
import com.ph.phpictureback.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 健康检查
 */
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    /**
     * 创建个人空间
     *
     * @param spaceAddDto
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddDto spaceAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddDto == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long spaceId = spaceService.addSpace(spaceAddDto, loginUser);
        return ResultUtils.success(spaceId);
    }


    /**
     * 分页获取空间信息
     *
     * @param spaceQueryDto
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN)
    @PostMapping("/list/space")
    public BaseResponse<Page<Space>> getListSpace(@RequestBody SpaceQueryDto spaceQueryDto) {
        ThrowUtils.throwIf(spaceQueryDto == null, ErrorCode.PARAMS_ERROR);
        int current = spaceQueryDto.getCurrent();
        int pageSize = spaceQueryDto.getPageSize();
        Page<Space> page = spaceService.page(new Page<>(current, pageSize),
                spaceService.getQueryWrapper(spaceQueryDto));
        return ResultUtils.success(page);
    }

    /**
     * 分页获取空间信息(脱敏)
     *
     * @param spaceQueryDto
     * @return
     */
    @PostMapping("/list/space/vo")
    public BaseResponse<Page<SpaceVO>> getListSpaceByVo(@RequestBody SpaceQueryDto spaceQueryDto,
                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceQueryDto == null, ErrorCode.PARAMS_ERROR);
        int current = spaceQueryDto.getCurrent();
        int pageSize = spaceQueryDto.getPageSize();
        //防止爬虫
        if (pageSize > 20) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "图片请求过多");
        }

        Page<Space> page = spaceService.page(new Page<>(current, pageSize),
                spaceService.getQueryWrapper(spaceQueryDto));

        Page<SpaceVO> pageVoList = spaceService.listSpaceVo(page, request);
        return ResultUtils.success(pageVoList);
    }


    /**
     * 根据id获取空间信息
     *
     * @param id
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN)
    @GetMapping("/get")
    public BaseResponse<Space> getSpaceById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间信息为空");
        return ResultUtils.success(space);
    }

    /**
     * 根据id获取空间信息(脱敏)
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceByIdVo(long id,HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间信息为空");
        User loginUser = userService.getLoginUser(request);
        //返回当前用户在空间中的权限
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        SpaceVO spaceVO = spaceService.getUserBySpace(space);
        spaceVO.setPermissionList(permissionList);
        return ResultUtils.success(spaceVO);
    }


    /**
     * 更新空间信息
     *
     * @param spaceUpdateDto
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateDto spaceUpdateDto) {
        ThrowUtils.throwIf(spaceUpdateDto == null, ErrorCode.PARAMS_ERROR);

        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateDto, space);

        spaceService.validSpace(space, false);
        spaceService.fillSpaceData(space);

        //观察空间是否为空
        Long spaceId = spaceUpdateDto.getId();
        Space oldSpace = spaceService.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.PARAMS_ERROR, "空间为空");

        boolean update = spaceService.updateById(space);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新失败");
        return ResultUtils.success(true);
    }

    /**
     * 删除空间
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);

        Long spaceId = deleteRequest.getId();
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
        //只有管理员可以删除
        User loginUser = userService.getLoginUser(request);
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean delete = spaceService.removeById(spaceId);

        ThrowUtils.throwIf(!delete, ErrorCode.SYSTEM_ERROR, "删除失败");
        return ResultUtils.success(true);
    }


    /**
     * 修改空间
     *
     * @param spaceEditDto
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditDto spaceEditDto, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceEditDto == null, ErrorCode.PARAMS_ERROR);
        //检验空间是否存在
        Space oldSpace = spaceService.getById(spaceEditDto.getId());
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.PARAMS_ERROR);
        //设置空间信息
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditDto, space);
        space.setEditTime(new Date());
        //检验空间信息
        spaceService.validSpace(space, false);
        spaceService.fillSpaceData(space);
        //本人可以修改
        User loginUser = userService.getLoginUser(request);
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean update = spaceService.updateById(space);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 获取所有的空间等级信息
     *
     * @return
     */
    @GetMapping("/get/level")
    public BaseResponse<List<SpaceLevel>> getSpaceLevel() {
        List<SpaceLevel> spaceLevels = Arrays.stream(SpaceLevelEnum.values()) //得到所有的枚举
                .map(spaceLevelEnum -> new SpaceLevel(spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize())).collect(Collectors.toList());
        return ResultUtils.success(spaceLevels);

    }


}

package com.ph.phpictureback.controller;

import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.auth.annotation.SaSpaceCheckPermission;
import com.ph.phpictureback.manager.auth.model.SpaceUserPermissionConstant;
import com.ph.phpictureback.model.dto.spaceuser.SpaceUserAddDto;
import com.ph.phpictureback.model.dto.spaceuser.SpaceUserEditDto;
import com.ph.phpictureback.model.dto.spaceuser.SpaceUserQueryDto;
import com.ph.phpictureback.model.entry.SpaceUser;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.SpaceUserVO;
import com.ph.phpictureback.service.SpaceUserService;
import com.ph.phpictureback.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 健康检查
 */
@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {
    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    /**
     * 添加空间用户
     *
     * @param spaceUserAddDto
     * @return
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddDto spaceUserAddDto) {
        ThrowUtils.throwIf(spaceUserAddDto == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        Long spaceUserId = spaceUserService.addSpaceUser(spaceUserAddDto);
        return ResultUtils.success(spaceUserId);
    }


    /**
     * 获取空间成员信息
     *
     * @param spaceUserQueryDto
     * @return
     */
    @PostMapping("/list/spaceuser")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUser>> getListSpaceUser(@RequestBody SpaceUserQueryDto spaceUserQueryDto) {
        ThrowUtils.throwIf(spaceUserQueryDto == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryDto));
        return ResultUtils.success(spaceUserList);
    }

    /**
     * 获取空间中成员信息
     *
     * @param spaceUserQueryDto
     * @return
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryDto spaceUserQueryDto) {
        ThrowUtils.throwIf(spaceUserQueryDto == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryDto.getSpaceId();
        Long userId = spaceUserQueryDto.getUserId();
        ThrowUtils.throwIf(spaceId == null || userId == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = spaceUserService.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, userId)
                .one();
        return ResultUtils.success(spaceUser);
    }


    /**
     * 分页获取空间成员信息(脱敏)
     *
     * @param spaceUserQueryDto
     * @return
     */
    @PostMapping("/list/spaceuser/vo")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> getListSpaceUserByVo(@RequestBody SpaceUserQueryDto spaceUserQueryDto,
                                                                HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryDto == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryDto));
        List<SpaceUserVO> spaceUserVOList = spaceUserService.listSpaceUserVo(spaceUserList);
        return ResultUtils.success(spaceUserVOList);
    }


    /**
     * 删除空间成员
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);

        Long spaceUserId = deleteRequest.getId();
        SpaceUser spaceUser = spaceUserService.getById(spaceUserId);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR, "空间成员不存在");

        boolean delete = spaceUserService.removeById(spaceUserId);

        ThrowUtils.throwIf(!delete, ErrorCode.SYSTEM_ERROR, "删除失败");
        return ResultUtils.success(true);
    }


    /**
     * 修改空间成员
     *
     * @param spaceUserEditDto
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditDto spaceUserEditDto) {
        ThrowUtils.throwIf(spaceUserEditDto == null, ErrorCode.PARAMS_ERROR);
        //检验空间成员是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(spaceUserEditDto.getId());
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.PARAMS_ERROR);
        //设置空间成员信息
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditDto, spaceUser);
        //检验空间信息
        spaceUserService.validSpaceUser(spaceUser, false);

        boolean update = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取我加入的团队
     *
     * @param request
     * @return
     */
    @GetMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMySpaceUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        SpaceUserQueryDto spaceUserQueryDto = new SpaceUserQueryDto();
        spaceUserQueryDto.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryDto));
        return ResultUtils.success(spaceUserService.listSpaceUserVo(spaceUserList));
    }
}

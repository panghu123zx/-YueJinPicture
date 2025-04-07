package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.model.dto.space.SpaceAddDto;
import com.ph.phpictureback.model.dto.space.SpaceQueryDto;
import com.ph.phpictureback.model.dto.spaceuser.SpaceUserAddDto;
import com.ph.phpictureback.model.dto.spaceuser.SpaceUserQueryDto;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.SpaceUserVO;
import com.ph.phpictureback.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 杨志亮
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-03-27 16:53:57
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 添加空间成员
     * @param spaceUserAddDto
     * @return
     */
    Long addSpaceUser(SpaceUserAddDto spaceUserAddDto);


    /**
     * 验证空间成员信息
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser, Boolean add);


    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryDto spaceUserQueryDto);


    SpaceUserVO getUserBySpaceUser(SpaceUser spaceUser);


    List<SpaceUserVO> listSpaceUserVo(List<SpaceUser> spaceUserList);
}

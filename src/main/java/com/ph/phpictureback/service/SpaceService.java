package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.model.dto.space.SpaceAddDto;
import com.ph.phpictureback.model.dto.space.SpaceQueryDto;
import com.ph.phpictureback.model.entry.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 杨志亮
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-03-19 16:58:18
*/
public interface SpaceService extends IService<Space> {

    /**
     * 添加空间
     * @param spaceAddDto
     * @param loginUser
     * @return
     */
    Long addSpace(SpaceAddDto spaceAddDto, User loginUser);


    /**
     * 验证空间信息
     * @param space
     * @param add
     */
    void validSpace(Space space,Boolean add);

    /**
     * 填充空间数据
     * @param space
     * @return
     */
    void fillSpaceData(Space space);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryDto spaceQueryDto);


    SpaceVO getUserBySpace(Space space);




    Page<SpaceVO> listSpaceVo(Page<Space> page, HttpServletRequest request);

    void checkSpaceAuth(Space space,User loginUser);
}

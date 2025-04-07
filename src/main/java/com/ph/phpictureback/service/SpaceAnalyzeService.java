package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.model.dto.analyze.*;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.space.analyze.*;

import java.util.List;

/**
 * @author 杨志亮
 * @description 针对表【picture(分析表)】的数据库操作Service
 * @createDate 2025-03-06 22:04:22
 */
public interface SpaceAnalyzeService extends IService<Picture> {
    /**
     * 空间使用情况分析
     *
     * @param spaceUsageAnalyzeDto
     * @param loginUser
     * @return
     */
    SpaceUsageAnalyzeVo spaceUsageAnalyze(SpaceUsageAnalyzeDto spaceUsageAnalyzeDto, User loginUser);

    /**
     * 按照图片分类分析图片情况
     *
     * @param spaceCategoryAnalyzeDto
     * @param loginUser
     * @return
     */
    List<SpaceCategoryAnalyzeVo> spaceCategoryAnalyze(SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto, User loginUser);


    /**
     * 根据标签对图片分类
     *
     * @param spaceTagAnalyzeDto
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeVo> spaceTagAnalyze(SpaceTagAnalyzeDto spaceTagAnalyzeDto, User loginUser);

    /**
     * 图片大小 空间分析
     *
     * @param spaceSizeAnalyzeDto
     * @param loginUser
     * @return
     */
    List<SpaceSizeAnalyzeVo> spaceSizeAnalyze(SpaceSizeAnalyzeDto spaceSizeAnalyzeDto, User loginUser);

    /**
     * 用户上传图片情况分析
     *
     * @param spaceUserAnalyzeDto
     * @param loginUser
     * @return
     */
    List<SpaceUserAnalyzeVo> spaceUserAnalyze(SpaceUserAnalyzeDto spaceUserAnalyzeDto, User loginUser);

    /**
     * 获取排名前N个空间
     *
     * @param spaceRankAnalyzeDto
     * @param loginUser
     * @return
     */
    List<Space> spaceAnalyze(SpaceRankAnalyzeDto spaceRankAnalyzeDto, User loginUser);
}

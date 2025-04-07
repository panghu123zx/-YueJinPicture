package com.ph.phpictureback.controller;

import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.analyze.*;
import com.ph.phpictureback.model.entry.Space;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.space.analyze.*;
import com.ph.phpictureback.service.SpaceAnalyzeService;
import com.ph.phpictureback.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 健康检查
 */
@RestController
@RequestMapping("/analyze")
public class SpaceAnalyzeController {
    @Resource
    private UserService userService;
    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;


    /**
     * 空间使用情况分析
     *
     * @param spaceUsageAnalyzeDto
     * @param request
     * @return
     */
    @PostMapping("/spaceusage")
    public BaseResponse<SpaceUsageAnalyzeVo> spaceUsageAnalyze(@RequestBody SpaceUsageAnalyzeDto spaceUsageAnalyzeDto,
                                                               HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeVo spaceUsageAnalyzeVo = spaceAnalyzeService.spaceUsageAnalyze(spaceUsageAnalyzeDto, loginUser);
        return ResultUtils.success(spaceUsageAnalyzeVo);
    }

    /**
     * 按照图片分类分析图片情况
     * @param spaceCategoryAnalyzeDto
     * @param request
     * @return
     */
    @PostMapping("/category/analyze")
    public BaseResponse<List<SpaceCategoryAnalyzeVo>> spaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeDto spaceCategoryAnalyzeDto
            ,HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeVo> spaceCategoryAnalyzeVoList = spaceAnalyzeService.spaceCategoryAnalyze(spaceCategoryAnalyzeDto, loginUser);
        return ResultUtils.success(spaceCategoryAnalyzeVoList);
    }

    /**
     * 根据标签对图片分类
     * @param spaceTagAnalyzeDto
     * @param request
     * @return
     */
    @PostMapping("/tag/analyze")
    public BaseResponse<List<SpaceTagAnalyzeVo>> spaceTagAnalyze(@RequestBody SpaceTagAnalyzeDto spaceTagAnalyzeDto,HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeVo> spaceTagAnalyzeVoList = spaceAnalyzeService.spaceTagAnalyze(spaceTagAnalyzeDto, loginUser);
        return ResultUtils.success(spaceTagAnalyzeVoList);
    }

    /**
     * 图片大小 空间分析
     * @param spaceSizeAnalyzeDto
     * @param request
     * @return
     */
    @PostMapping("/size/analyze")
    public BaseResponse<List<SpaceSizeAnalyzeVo>> spaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeDto spaceSizeAnalyzeDto, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeVo> spaceSizeAnalyzeVoList = spaceAnalyzeService.spaceSizeAnalyze(spaceSizeAnalyzeDto, loginUser);
        return ResultUtils.success(spaceSizeAnalyzeVoList);
    }

    /**
     * 用户上传图片情况分析
     * @param spaceUserAnalyzeDto
     * @param request
     * @return
     */
    @PostMapping("/user/analyze")
    public BaseResponse<List<SpaceUserAnalyzeVo>> spaceUserAnalyze(@RequestBody SpaceUserAnalyzeDto spaceUserAnalyzeDto, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserAnalyzeVo> spaceUserAnalyzeVoList = spaceAnalyzeService.spaceUserAnalyze(spaceUserAnalyzeDto, loginUser);
        return ResultUtils.success(spaceUserAnalyzeVoList);
    }

    /**
     * 空间使用情况分析，只取前 N 名
     * @param spaceRankAnalyzeDto
     * @param request
     * @return
     */
    @PostMapping("/rank")
    public BaseResponse<List<Space>> spaceAnalyze(@RequestBody SpaceRankAnalyzeDto spaceRankAnalyzeDto, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        List<Space> spaceList = spaceAnalyzeService.spaceAnalyze(spaceRankAnalyzeDto, loginUser);
        return ResultUtils.success(spaceList);
    }
}

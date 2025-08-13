package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.api.aliyun.model.CreateOutPaintingTaskVo;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.model.dto.picture.*;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

/**
 * @author 杨志亮
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-03-10 22:21:48
 */
public interface PictureService extends IService<Picture> {


    /**
     * 上传图片
     *
     * @param inputSource      文件
     * @param pictureUploadDto 图片id
     * @param loginuser        登入用户
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadDto pictureUploadDto, User loginuser);


    /**
     * 分页获取图片列表
     *
     * @param page
     * @param request
     * @return
     */
    Page<PictureVO> listPictureVo(Page<Picture> page, HttpServletRequest request);


    /**
     * 条件查询请求
     *
     * @param pictureQueryDto
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryDto pictureQueryDto);


    /**
     * 更新图片信息
     *
     * @param pictureUpdateDto
     * @return
     */
    boolean updatePicture(PictureUpdateDto pictureUpdateDto);

    /**
     * 删除图片
     * @param deleteRequest
     * @return
     */
    boolean deletePicture(DeleteRequest deleteRequest);

    /**
     * 编辑图片
     * @param pictureEditDto
     * @param loginUser
     * @return
     */
    boolean editPicture(PictureEditDto pictureEditDto,User loginUser);

    /**
     * 获取用户信息
     *
     * @param picture
     * @return
     */
    PictureVO getUserByPicture(Picture picture);

    /**
     * 检验图片信息
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewDto
     * @param loginUser
     */
    void pictureReview(PictureReviewDto pictureReviewDto, User loginUser);

    /**
     * 获取脱敏的图片
     *
     * @param id
     * @param loginUser
     * @return
     */
    PictureVO getPictureVo(long id, User loginUser);


    /**
     * 审核信息的填写
     *
     * @param picture
     * @param loginUser
     */
    void sendReviewMessage(Picture picture, User loginUser);


    /**
     * 批量插入图片
     *
     * @param pictureUploadByBatchDto
     * @param loginUser
     * @return
     */
    Integer uploadPictureByBatch(PictureUploadByBatchDto pictureUploadByBatchDto, User loginUser);

    /**
     * 根据颜色搜索图片
     *
     * @param spaceId
     * @param color
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(long spaceId, String color, User loginUser);

    /**
     * 批量修改图片信息
     *
     * @param pictureEditByBatchDto
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchDto pictureEditByBatchDto, User loginUser);

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskDto
     * @param loginUser
     */
    CreateOutPaintingTaskVo createTask(CreatePictureOutPaintingTaskDto createPictureOutPaintingTaskDto, User loginUser);


    /**
     * 清理COS 中的数据
     *
     * @param picture
     */
    void cosDeletePicture(Picture picture);

    Page<PictureVO> getFollowPicture(PictureQueryDto pictureQueryDto,HttpServletRequest request);

    /**
     * 批量处理点赞信息
     * @param map
     * @return
     */
    boolean batchUpdatePictureLike(HashMap<Long, Long> map);

    /**
     * 校验图片权限 （已使用so-token校验）
     * @param picture
     * @param loginUser
     */
//    void checkPictureAuth(Picture picture, User loginUser);


}

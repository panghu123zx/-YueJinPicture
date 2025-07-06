package com.ph.phpictureback.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.annotation.AuthCheck;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.forum.ForumAddDto;
import com.ph.phpictureback.model.dto.forum.ForumQueryDto;
import com.ph.phpictureback.model.dto.forum.ForumReviewDto;
import com.ph.phpictureback.model.dto.forum.ForumUpdateDto;
import com.ph.phpictureback.model.dto.forumFile.ForumFileAddDto;
import com.ph.phpictureback.model.dto.forumFile.ForumFileDeleteDto;
import com.ph.phpictureback.model.dto.forumFile.ForumFileQueryDto;
import com.ph.phpictureback.model.dto.picture.PictureTagCategory;
import com.ph.phpictureback.model.entry.Forum;
import com.ph.phpictureback.model.entry.ForumFile;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.ForumVO;
import com.ph.phpictureback.service.ForumFileService;
import com.ph.phpictureback.service.ForumService;
import com.ph.phpictureback.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * 论坛
 */
@RestController
@RequestMapping("/forum")
public class ForumController {
    @Resource
    private ForumService forumService;
    @Resource
    private UserService userService;
    @Resource
    private ForumFileService forumFileService;

    /**
     * 添加论坛
     * @param forumAddDto
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Boolean> addForum(@RequestBody ForumAddDto forumAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(forumAddDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        boolean update = forumService.addForum(forumAddDto, loginUser);
        return ResultUtils.success(update);
    }

    /**
     * 删除论坛
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteForum(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        boolean update = forumService.deleteForum(deleteRequest.getId(), loginUser);
        return ResultUtils.success(update);
    }

    /**
     * 修改论坛
     * @param forumUpdateDto
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateForum(@RequestBody ForumUpdateDto forumUpdateDto, HttpServletRequest request) {
        ThrowUtils.throwIf(forumUpdateDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        boolean update = forumService.updateForum(forumUpdateDto,loginUser);
        return ResultUtils.success(update);
    }

    /**
     * 分页查询论坛
     * @param forumQueryDto
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/list")
    public BaseResponse<Page<Forum>> listForum(@RequestBody ForumQueryDto forumQueryDto) {
        int current = forumQueryDto.getCurrent();
        int pageSize = forumQueryDto.getPageSize();
        Page<Forum> page = forumService.page(new Page<>(current, pageSize), forumService.getQueryWrapper(forumQueryDto));
        return ResultUtils.success(page);
    }

    /**
     * 分页查询论坛VO
     * @param forumQueryDto
     * @return
     */
    @PostMapping("/list/vo")
    public BaseResponse<Page<ForumVO>> listForumVo(@RequestBody ForumQueryDto forumQueryDto) {
        int current = forumQueryDto.getCurrent();
        int pageSize = forumQueryDto.getPageSize();
        if(pageSize> 50){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不允许查询过多数据");
        }
        Page<Forum> page = forumService.page(new Page<>(current, pageSize), forumService.getQueryWrapper(forumQueryDto));
        Page<ForumVO> forumVOPage = forumService.listForumVO(page);
        return ResultUtils.success(forumVOPage);
    }

    /**
     * 获取forum
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Forum> getForum(long id){
        ThrowUtils.throwIf(ObjectUtil.isEmpty(id), ErrorCode.PARAMS_ERROR, "参数错误");
        Forum forum = forumService.getById(id);
        return ResultUtils.success(forum);
    }

    /**
     * 获取forumVO
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<ForumVO> getForumVO(long id){
        ThrowUtils.throwIf(ObjectUtil.isEmpty(id), ErrorCode.PARAMS_ERROR, "参数错误");
        ForumVO forum = forumService.getForumVO(id);
        return ResultUtils.success(forum);
    }

    /**
     * 审核帖子
     * @param forumReviewDto
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/review")
    public BaseResponse<Boolean> reviewForum(@RequestBody ForumReviewDto forumReviewDto,HttpServletRequest request) {
        ThrowUtils.throwIf(forumReviewDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        boolean update = forumService.reviewForum(forumReviewDto, loginUser);
        return ResultUtils.success(update);
    }

    /**
     * 获取我发布的帖子
     * @param forumQueryDto
     * @param request
     * @return
     */
    @PostMapping("/list/myforum/vo")
    public BaseResponse<Page<ForumVO>> getMyListForumByVo(@RequestBody ForumQueryDto forumQueryDto,
                                                          HttpServletRequest request) {
        ThrowUtils.throwIf(forumQueryDto == null, ErrorCode.PARAMS_ERROR,"参数错误");
        int current = forumQueryDto.getCurrent();
        int pageSize = forumQueryDto.getPageSize();
        //防止爬虫
        if (pageSize > 20) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "请求过多");
        }
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!loginUser.getId().equals(forumQueryDto.getUserId()), ErrorCode.NO_AUTH_ERROR);
        Page<Forum> page = forumService.page(new Page<>(current, pageSize),
                forumService.getQueryWrapper(forumQueryDto));
        Page<ForumVO> pageVoList = forumService.listForumVO(page);
        return ResultUtils.success(pageVoList);
    }

    //帖子文件的创建

    /**
     * 创建帖子文件
     * @param multipartFile
     * @param forumFileAddDto
     * @param request
     * @return
     */
    @PostMapping("/addfile")
    public BaseResponse<ForumFile> addForumFile(@RequestPart("file") MultipartFile multipartFile, ForumFileAddDto forumFileAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(forumFileAddDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        ForumFile forumFile = forumService.addForumFile(multipartFile,forumFileAddDto,loginUser);
        return ResultUtils.success(forumFile);
    }

    /**
     * 创建帖子文件url
     * @param forumFileAddDto
     * @param request
     * @return
     */
    @PostMapping("/addfileurl")
    public BaseResponse<ForumFile> addForumFileUrl(@RequestBody ForumFileAddDto forumFileAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(forumFileAddDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        User loginUser = userService.getLoginUser(request);
        ForumFile forumFile = forumService.addForumFile(forumFileAddDto.getFileUrl(),forumFileAddDto,loginUser);
        return ResultUtils.success(forumFile);
    }

    /**
     * 查询帖子文件
     * @param forumFileQueryDto
     * @param request
     * @return
     */
    @PostMapping("/queryfile")
    public BaseResponse<List<ForumFile>> queryForumFile(@RequestBody ForumFileQueryDto forumFileQueryDto, HttpServletRequest request) {
        ThrowUtils.throwIf(forumFileQueryDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        userService.getLoginUser(request);
        List<ForumFile> forumFile = forumService.queryForumFile(forumFileQueryDto);
        return ResultUtils.success(forumFile);
    }


    /**
     * 删除对应的帖子文件 集合
     * @param forumFileDeleteDto
     * @param request
     * @return
     */
    @PostMapping("/deletefilelist")
    public BaseResponse<Boolean> deleteForumFileList(@RequestBody ForumFileDeleteDto forumFileDeleteDto, HttpServletRequest request) {
        ThrowUtils.throwIf(forumFileDeleteDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        userService.getLoginUser(request);
        List<Long> ids = forumFileDeleteDto.getIds();
        boolean update = forumFileService.removeByIds(ids);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 删除帖子文件
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/deletefile")
    public BaseResponse<Boolean> deleteForumFile(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest.getId() <=0, ErrorCode.PARAMS_ERROR, "参数错误");
        userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        ForumFile byId = forumFileService.getById(id);
        ThrowUtils.throwIf(byId == null, ErrorCode.PARAMS_ERROR, "文件不存在");
        boolean update = forumFileService.removeById(id);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 标签信息
     *
     * @return
     */
    @GetMapping("/category")
    public BaseResponse<PictureTagCategory> listForumCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> categoryList = Arrays.asList("生活", "科幻", "技巧", "美食", "日常", "网络","体育","时尚","摄影","旅游");
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }
}

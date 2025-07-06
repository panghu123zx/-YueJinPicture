package com.ph.phpictureback.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.common.PageRequest;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.comment.AddCommentDto;
import com.ph.phpictureback.model.dto.comment.CommentQueryDto;
import com.ph.phpictureback.model.dto.comment.CommentReadDto;
import com.ph.phpictureback.model.dto.comment.LikeCommentDto;
import com.ph.phpictureback.model.entry.Comment;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.ForumPictureTypeEnum;
import com.ph.phpictureback.model.vo.CommentVO;
import com.ph.phpictureback.service.CommentService;
import com.ph.phpictureback.service.ForumService;
import com.ph.phpictureback.service.PictureService;
import com.ph.phpictureback.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论功能
 */
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Resource
    private CommentService commentService;
    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private ForumService forumService;


    /**
     * 获取目标的图片的评论
     *
     * @param commentQueryDto
     * @param request
     * @return
     */
    @PostMapping("/get/target")
    public BaseResponse<List<CommentVO>> getTargetComment(@RequestBody CommentQueryDto commentQueryDto, HttpServletRequest request) {
        ThrowUtils.throwIf(commentQueryDto == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        User loginUser = userService.getLoginUser(request);
        List<CommentVO> commentVOList = commentService.getAllTargetComment(commentQueryDto, loginUser);
        return ResultUtils.success(commentVOList);
    }

    /**
     * 删除评论
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteComment(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() < 0, ErrorCode.PARAMS_ERROR, "参数不能为空");
        User loginUser = userService.getLoginUser(request);
        boolean update = commentService.deleteCommentById(deleteRequest, loginUser);
        return ResultUtils.success(update);
    }

    /**
     * 添加评论
     * @param addCommentDto
     * @param request
     * @return
     */
    @PostMapping
    public BaseResponse<Boolean> addComment(@RequestBody AddCommentDto addCommentDto, HttpServletRequest request) {
        ThrowUtils.throwIf(addCommentDto == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        User loginUser = userService.getLoginUser(request);
        boolean add = commentService.addComment(addCommentDto,loginUser);
        return ResultUtils.success(add);
    }

    /**
     * 点赞评论
     * @param likeCommentDto
     * @param request
     * @return
     */
    @PostMapping("/like/comment")
    public BaseResponse<Boolean> likeComment(@RequestBody LikeCommentDto likeCommentDto, HttpServletRequest request) {
        ThrowUtils.throwIf(likeCommentDto == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        User loginUser = userService.getLoginUser(request);
        Long id = likeCommentDto.getId();
        Comment comment = commentService.getById(id);
        ThrowUtils.throwIf(comment == null, ErrorCode.PARAMS_ERROR, "评论不存在");
        boolean update = commentService.lambdaUpdate()
                .eq(Comment::getId, id)
                .setSql("likeCount = likeCount + 1")
                .update();
        return ResultUtils.success(update);
    }


    /**
     * 获取我的评论历史
     *
     * @param commentQueryDto
     * @param request
     * @return
     */
    @PostMapping("/getMy/history")
    public BaseResponse<Page<CommentVO>> getMyCommentHistory(@RequestBody CommentQueryDto commentQueryDto, HttpServletRequest request) {
        int current = commentQueryDto.getCurrent();
        int pageSize = commentQueryDto.getPageSize();
        Integer targetType = commentQueryDto.getTargetType();
        ForumPictureTypeEnum forumPictureTypeValue = ForumPictureTypeEnum.getForumPictureTypeValue(targetType);
        ThrowUtils.throwIf(forumPictureTypeValue == null, ErrorCode.PARAMS_ERROR, "参数错误");

        User loginUser = userService.getLoginUser(request);

        QueryWrapper<Comment> qw = new QueryWrapper<>();
        qw.eq("userId", loginUser.getId());
        qw.eq(ObjectUtil.isNotNull(forumPictureTypeValue.getValue()),"targetType",targetType);
        //查询到了所有的评论过 我 的图片和帖子的评论
        List<Comment> list = commentService.list(qw);
        List<CommentVO> commentVOList = list.stream().map(CommentVO::objToVo).collect(Collectors.toList());
        commentVOList.forEach(commentVO -> {
            Long targetId = commentVO.getTargetId();
            if(targetType.equals(ForumPictureTypeEnum.PICTURE.getValue())){
                commentVO.setPictureVO(pictureService.getPictureVo(targetId, loginUser));
            }else{
                commentVO.setForumVO(forumService.getForumVO(targetId));
            }
        });

        Page<CommentVO> page = new Page<>(current, pageSize, list.size());
        page.setRecords(commentVOList);
        return ResultUtils.success(page);
    }

    /**
     * 获取评论我的历史
     *
     * @param commentQueryDto
     * @param request
     * @return
     */
    @PostMapping("/get/historyMy")
    public BaseResponse<Page<CommentVO>> getCommentHistoryMy(@RequestBody CommentQueryDto commentQueryDto, HttpServletRequest request) {
        int current = commentQueryDto.getCurrent();
        int pageSize = commentQueryDto.getPageSize();
        Integer isRead = commentQueryDto.getIsRead();
        Integer targetType = commentQueryDto.getTargetType();
        ForumPictureTypeEnum forumPictureTypeValue = ForumPictureTypeEnum.getForumPictureTypeValue(targetType);
        ThrowUtils.throwIf(forumPictureTypeValue == null, ErrorCode.PARAMS_ERROR, "参数错误");

        User loginUser = userService.getLoginUser(request);

        QueryWrapper<Comment> qw = new QueryWrapper<>();
        qw.eq("fromId", loginUser.getId());
        qw.eq(ObjectUtil.isNotNull(isRead),"isRead",isRead);
        qw.eq(ObjectUtil.isNotNull(forumPictureTypeValue.getValue()),"targetType",targetType);
        //查询到了所有的评论过 我 的图片和帖子的评论
        List<Comment> list = commentService.list(qw);
        List<CommentVO> commentVOList = list.stream().map(CommentVO::objToVo).collect(Collectors.toList());
        commentVOList.forEach(commentVO -> {
            Long targetId = commentVO.getTargetId();
            if(targetType.equals(ForumPictureTypeEnum.PICTURE.getValue())){
                commentVO.setPictureVO(pictureService.getPictureVo(targetId, loginUser));
            }else{
                commentVO.setForumVO(forumService.getForumVO(targetId));
            }
        });

        Page<CommentVO> page = new Page<>(current, pageSize, list.size());
        page.setRecords(commentVOList);
        return ResultUtils.success(page);
    }


    /**
     * 读取评论
     * @param commentReadDto
     * @param request
     * @return
     */
    @PostMapping("/read")
    public BaseResponse<Boolean> readComment(@RequestBody CommentReadDto commentReadDto, HttpServletRequest request){
        ThrowUtils.throwIf(commentReadDto ==null, ErrorCode.PARAMS_ERROR,"参数错误");
        userService.getLoginUser(request);
        boolean read = commentService.readComment(commentReadDto);
        return ResultUtils.success(read);
    }
}

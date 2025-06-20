package com.ph.phpictureback.controller;

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
import com.ph.phpictureback.model.dto.comment.LikeCommentDto;
import com.ph.phpictureback.model.entry.Comment;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.CommentVO;
import com.ph.phpictureback.service.CommentService;
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
     * @param pageRequest
     * @param request
     * @return
     */
    @PostMapping("/getMy/history")
    public BaseResponse<Page<CommentVO>> getMyCommentHistory(@RequestBody PageRequest pageRequest, HttpServletRequest request) {
        int current = pageRequest.getCurrent();
        int pageSize = pageRequest.getPageSize();
        User loginUser = userService.getLoginUser(request);

        QueryWrapper<Comment> qw = new QueryWrapper<>();
        qw.eq("userId", loginUser.getId());
        List<Comment> list = commentService.list(qw);
        List<CommentVO> commentVOList = list.stream().map(CommentVO::objToVo).collect(Collectors.toList());
        Page<CommentVO> page = new Page<>(current, pageSize, list.size());
        page.setRecords(commentVOList);
        return ResultUtils.success(page);
    }

    /**
     * 获取评论我的历史
     *
     * @param pageRequest
     * @param request
     * @return
     */
    @PostMapping("/get/historyMy")
    public BaseResponse<Page<CommentVO>> getCommentHistoryMy(@RequestBody PageRequest pageRequest, HttpServletRequest request) {
        int current = pageRequest.getCurrent();
        int pageSize = pageRequest.getPageSize();
        User loginUser = userService.getLoginUser(request);

        QueryWrapper<Comment> qw = new QueryWrapper<>();
        qw.eq("fromId", loginUser.getId());
        List<Comment> list = commentService.list(qw);
        List<CommentVO> commentVOList = list.stream().map(CommentVO::objToVo).collect(Collectors.toList());
        Page<CommentVO> page = new Page<>(current, pageSize, list.size());
        page.setRecords(commentVOList);
        return ResultUtils.success(page);
    }
}

package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.model.dto.comment.AddCommentDto;
import com.ph.phpictureback.model.dto.comment.CommentQueryDto;
import com.ph.phpictureback.model.entry.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.CommentVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 杨志亮
* @description 针对表【comment(评论)】的数据库操作Service
* @createDate 2025-04-01 10:18:07
*/
public interface CommentService extends IService<Comment> {

    /**
     * 获取目标评论
     * @param commentQueryDto
     * @param loginUser
     * @return
     */
    List<CommentVO> getAllTargetComment(CommentQueryDto commentQueryDto, User loginUser);

    /**
     * 删除评论
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    boolean deleteCommentById(DeleteRequest deleteRequest, User loginUser);

    /**
     * 添加评论
     * @param addCommentDto
     * @param loginUser
     * @return
     */
    boolean addComment(AddCommentDto addCommentDto,User loginUser);

}

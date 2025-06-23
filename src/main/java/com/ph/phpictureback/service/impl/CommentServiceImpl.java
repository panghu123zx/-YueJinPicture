package com.ph.phpictureback.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.mapper.CommentMapper;
import com.ph.phpictureback.model.dto.comment.AddCommentDto;
import com.ph.phpictureback.model.dto.comment.CommentQueryDto;
import com.ph.phpictureback.model.entry.Comment;
import com.ph.phpictureback.model.entry.Forum;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.CommentVO;
import com.ph.phpictureback.model.vo.UserVO;
import com.ph.phpictureback.service.CommentService;
import com.ph.phpictureback.service.ForumService;
import com.ph.phpictureback.service.PictureService;
import com.ph.phpictureback.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 杨志亮
 * @description 针对表【comment(评论)】的数据库操作Service实现
 * @createDate 2025-04-01 10:18:07
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
        implements CommentService {


    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private ForumService forumService;

    /**
     * 根据题目id获取到所有的评论
     *
     * @param commentQueryDto
     * @param loginUser
     * @return
     */
    @Override
    public List<CommentVO> getAllTargetComment(CommentQueryDto commentQueryDto, User loginUser) {
        Long targetId = commentQueryDto.getTargetId();
        Integer targetType = commentQueryDto.getTargetType();
        //查找所有的父级评论
        QueryWrapper<Comment> qwFather = new QueryWrapper<>();
        qwFather.eq("targetId", targetId);
        qwFather.eq("targetType", targetType);
        qwFather.eq("parentId", -1);
        List<Comment> commentList = this.list(qwFather);
        List<CommentVO> commentVoList = new ArrayList<>();
        //转化为vo类型
        for (Comment comment : commentList) {
            CommentVO commentVO = CommentVO.objToVo(comment);
            //当前评论的user
            User user = userService.getById(comment.getUserId());
            UserVO userVO = UserVO.objToVo(user);
            commentVO.setUserVO(userVO);
            commentVoList.add(commentVO);
        }
        //根据父级评论查询子级评论
        for (CommentVO commentVO : commentVoList) {
            QueryWrapper<Comment> qwChild = new QueryWrapper<>();
            qwChild.eq("parentId", commentVO.getId());

            List<Comment> commentListChild = this.list(qwChild);
            List<CommentVO> commentVOChildList = new ArrayList<>();
            for (Comment comment : commentListChild) {
                CommentVO commentVOChild = CommentVO.objToVo(comment);
                //子级评论的user
                User user = userService.getById(comment.getUserId());
                UserVO userVO = UserVO.objToVo(user);
                commentVOChild.setUserVO(userVO);
                commentVOChildList.add(commentVOChild);
            }
            //将子级的评论放进去
            commentVO.setCommentVOChildList(commentVOChildList);
        }
        return commentVoList;
    }

    /**
     * 删除用户评论的一条内容，
     *
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean deleteCommentById(DeleteRequest deleteRequest, User loginUser) {
        Comment comment = new Comment();
        Long id = deleteRequest.getId();
        Comment commentById = this.getById(id);
        ThrowUtils.throwIf(commentById == null, ErrorCode.PARAMS_ERROR, "评论不存在");
        if (!commentById.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "没有权限删除");
        }
        Picture picture = null;
        Forum forum = null;
        Integer targetType = commentById.getTargetType();
        switch (targetType) {
            case 0:
                picture = pictureService.getById(commentById.getTargetId());
                ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片不存在");
                break;
            case 1:
                forum = forumService.getById(commentById.getTargetId());
                ThrowUtils.throwIf(forum == null, ErrorCode.PARAMS_ERROR, "帖子不存在");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论类型错误");
        }
        comment.setId(id);
        //如果没有子级评论就直接删除
        QueryWrapper<Comment> qw = new QueryWrapper<>();
        qw.eq("parentId", id);
        long childCount = this.count(qw);
        if (childCount == 0) {
            boolean remove = this.removeById(id);
            if (!remove) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
            }
            // todo  子级评论删除之后查看父级的如果父级的也被删除了，也将父级的删除了
        } else {
            //有子级评论就将内容更改为 “评论已被删除”
            comment.setContent("评论已被删除");
            boolean update = this.updateById(comment);
            if (!update) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
            }
        }
        if(targetType==0){
            updateCommentCountDelete(picture);
        }else{
            updateCommentCountDeleteByForum(forum);
        }

        return true;
    }


    /**
     * 添加评论
     *
     * @param addCommentDto
     * @return
     */
    @Override
    public boolean addComment(AddCommentDto addCommentDto, User loginUser) {
        if (StrUtil.isBlank(addCommentDto.getContent()) || addCommentDto.getContent().equals("评论已被删除")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        }

        Comment comment = new Comment();
        BeanUtils.copyProperties(addCommentDto, comment);
        comment.setUserId(loginUser.getId());
        //直接进行添加操作
        boolean save = this.save(comment);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加评论失败");
        }

        Integer targetType = addCommentDto.getTargetType();
        Long targetId = addCommentDto.getTargetId();
        //更新评论数
        switch (targetType) {
            case 0:
                Picture picture = pictureService.getById(targetId);
                updateCommentCount(picture);
                break;
            case 1:
                Forum forum = forumService.getById(targetId);
                updateCommentCountByForum(forum);
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论类型错误");
        }

        return true;
    }

    /**
     * 更新评论数
     *
     * @param picture
     */
    public void updateCommentCount(Picture picture) {
        //更新评论数
        if (picture != null) {
            boolean update = pictureService.lambdaUpdate()
                    .eq(Picture::getId, picture.getId())
                    .setSql("commentCount = commentCount + 1")
                    .update();
            ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新评论数失败");
        }
    }

    public void updateCommentCountDelete(Picture picture) {
        //更新评论数
        if (picture != null) {
            boolean update = pictureService.lambdaUpdate()
                    .eq(Picture::getId, picture.getId())
                    .setSql("commentCount = commentCount - 1")
                    .update();
            ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新评论数失败");
        }
    }


    /**
     * 更新帖子评论数
     *
     * @param forum
     */
    public void updateCommentCountByForum(Forum forum) {
        //更新评论数
        if (forum != null) {
            boolean update = forumService.lambdaUpdate()
                    .eq(Forum::getId, forum.getId())
                    .setSql("commentCount = commentCount + 1")
                    .update();
            ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新评论数失败");
        }
    }

    public void updateCommentCountDeleteByForum(Forum forum) {
        //更新评论数
        if (forum != null) {
            boolean update = forumService.lambdaUpdate()
                    .eq(Forum::getId, forum.getId())
                    .setSql("commentCount = commentCount - 1")
                    .update();
            ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新评论数失败");
        }
    }
}





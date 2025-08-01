package com.ph.phpictureback.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.mapper.CommentMapper;
import com.ph.phpictureback.model.dto.comment.AddCommentDto;
import com.ph.phpictureback.model.dto.comment.CommentQueryDto;
import com.ph.phpictureback.model.dto.comment.CommentReadDto;
import com.ph.phpictureback.model.entry.Comment;
import com.ph.phpictureback.model.entry.Forum;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.ForumPictureTypeEnum;
import com.ph.phpictureback.model.vo.CommentVO;
import com.ph.phpictureback.model.vo.PictureVO;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        //查找所有目标对象的的父级评论
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
        if (targetType == 0) {
            updateCommentCountDelete(picture);
        } else {
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
     * 读取评论
     *
     * @param commentReadDto
     * @return
     */
    @Override
    public boolean readComment(CommentReadDto commentReadDto) {
        Long id = commentReadDto.getId();
        Comment comment = this.getById(id);
        ThrowUtils.throwIf(comment == null, ErrorCode.PARAMS_ERROR, "评论不存在");
        comment.setIsRead(1);
        boolean update = this.updateById(comment);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "读取评论失败");
        return true;
    }

    /**
     * 查询用户评论
     *
     * @param page
     * @param loginUser
     */
    @Override
    public Page<CommentVO> commentMy(Page<Comment> page, User loginUser) {
        List<Comment> records = page.getRecords();
        Page<CommentVO> pageVO = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(records)) return pageVO;


        //转换成为VO类
        List<CommentVO> commentVOList = records.stream()
                .map(CommentVO::objToVo)
                .collect(Collectors.toList());

        //得到所有的评论了 我的 的人的id
        Set<Long> idSet = records.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());

        //得到用户id
        Map<Long, List<User>> userMap = userService.listByIds(idSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        commentVOList.forEach(commentVO -> {
            //设置评论人的信息
            Long userId = commentVO.getUserId();
            if (userMap.containsKey(userId)) {
                User user = userMap.get(userId).get(0);
                commentVO.setUserVO(UserVO.objToVo(user));
            }
            //设置目标信息
            Long targetId = commentVO.getTargetId();
            Integer targetType = commentVO.getTargetType();
            if (targetType.equals(ForumPictureTypeEnum.PICTURE.getValue())) {
                commentVO.setPictureVO(pictureService.getPictureVo(targetId, loginUser));
            } else {
                commentVO.setForumVO(forumService.getForumVO(targetId,loginUser));
            }
        });
        pageVO.setRecords(commentVOList);
        return pageVO;
    }

    @Override
    public QueryWrapper<Comment> getQueryWrapper(CommentQueryDto commentQueryDto) {
        ThrowUtils.throwIf(commentQueryDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        QueryWrapper<Comment> qw = new QueryWrapper<>();
        Long id = commentQueryDto.getId();
        Long targetId = commentQueryDto.getTargetId();
        Integer targetType = commentQueryDto.getTargetType();
        Long userId = commentQueryDto.getUserId();
        String content = commentQueryDto.getContent();
        Long parentId = commentQueryDto.getParentId();
        Integer likeCount = commentQueryDto.getLikeCount();
        Long fromId = commentQueryDto.getFromId();
        Integer isRead = commentQueryDto.getIsRead();
        String sortField = commentQueryDto.getSortField();
        String sortOrder = commentQueryDto.getSortOrder();
        Long targetUserId = commentQueryDto.getTargetUserId();
        Integer isCommentMy = commentQueryDto.getIsCommentMy();

        qw.eq(ObjectUtil.isNotNull(targetId), "targetId", targetId);
        qw.eq(ObjectUtil.isNotNull(targetType), "targetType", targetType);
        qw.eq(ObjectUtil.isNotNull(isRead), "isRead", isRead);
        qw.eq(ObjectUtil.isNotNull(id), "id", id);
        qw.eq(ObjectUtil.isNotNull(userId), "userId", userId);
        qw.eq(ObjectUtil.isNotNull(parentId), "parentId", parentId);
        qw.eq(ObjectUtil.isNotNull(likeCount), "likeCount", likeCount);
        qw.like(ObjectUtil.isNotNull(content), "content", content);
        //判断是评论我的消息，还是我发出的消息
        if(isCommentMy==0){
            qw.and(wrapper ->
                    wrapper.eq(ObjectUtil.isNotNull(fromId), "fromId", fromId)
                            .or(sub -> sub.isNull("fromId").eq("targetUserId", targetUserId)) //如果回复人为空，就恢复目标对象的创建人
            );
        }else if (isCommentMy==1){
            qw.eq(ObjectUtil.isNotNull(fromId), "fromId", fromId);
            qw.eq(ObjectUtil.isNotNull(targetUserId), "targetUserId", targetUserId);
        }else{
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"参数错误");
        }

        qw.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return qw;
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





package com.ph.phpictureback.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.DownloadFileTemplate;
import com.ph.phpictureback.manager.RedissonLock;
import com.ph.phpictureback.manager.fileUpload.FilePictureUpload;
import com.ph.phpictureback.manager.fileUpload.UrlPictureUpload;
import com.ph.phpictureback.manager.redisCache.ForumCache;
import com.ph.phpictureback.model.dto.forum.ForumAddDto;
import com.ph.phpictureback.model.dto.forum.ForumQueryDto;
import com.ph.phpictureback.model.dto.forum.ForumReviewDto;
import com.ph.phpictureback.model.dto.forum.ForumUpdateDto;
import com.ph.phpictureback.model.dto.picture.UploadPictureDto;
import com.ph.phpictureback.model.entry.Forum;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.ReviewStatusEnum;
import com.ph.phpictureback.model.vo.ForumVO;
import com.ph.phpictureback.service.ForumService;
import com.ph.phpictureback.mapper.ForumMapper;
import com.ph.phpictureback.service.UserService;
import jodd.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author 杨志亮
 * @description 针对表【forum(论坛表)】的数据库操作Service实现
 * @createDate 2025-06-23 11:50:19
 */
@Service
public class ForumServiceImpl extends ServiceImpl<ForumMapper, Forum>
        implements ForumService {
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private RedissonLock redissonLock;
    @Resource
    private UserService userService;

    @Resource
    private ForumCache forumCache;

    /**
     * 添加帖子
     *
     * @param forumAddDto
     * @param loginUser
     * @return
     */
    @Override
    public boolean addForum(Object inputSource, ForumAddDto forumAddDto, User loginUser) {
        Forum forum = new Forum();
        BeanUtils.copyProperties(forumAddDto, forum);
        //设置创建人
        Long userId = loginUser.getId();
        forum.setUserId(userId);
        //验证帖子信息
        if (forum.getTitle() == null || forum.getContent() == null || forum.getCategory() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子信息不完整");
        }
        if (forum.getTitle().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        //论坛可以不带有图片
        if (inputSource != null) {
            //上传图片
            DownloadFileTemplate pictureUpload = filePictureUpload;
            //判断是否为文url上传
            if (inputSource instanceof String) {
                pictureUpload = urlPictureUpload;
            }
            //图片存放地址
            String uploadPath = String.format("/forum/%s", userId);
            UploadPictureDto uploadPicture = pictureUpload.uploadPicture(inputSource, uploadPath);
            forum.setUrl(uploadPicture.getUrl());
            forum.setThumbnailUrl(uploadPicture.getThumbnailUrl());
        }
        //保存帖子
        Boolean b = redissonLock.lockExecute(String.valueOf(userId), () -> this.save(forum));
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR, "帖子创建失败");
        return true;
    }

    /**
     * 删除帖子
     *
     * @param id
     * @param loginUser
     * @return
     */
    @Override
    public boolean deleteForum(long id, User loginUser) {
        //判空
        Forum forum = this.getById(id);
        ThrowUtils.throwIf(forum == null, ErrorCode.PARAMS_ERROR, "帖子不存在");
        //判断是否为管理员
        if (!forum.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除该帖子");
        }
        //删除帖子
        Boolean b = redissonLock.lockExecute(String.valueOf(id), () -> this.removeById(id));
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR, "帖子删除失败");
        //todo 删除帖子带的图片
        return true;
    }

    /**
     * 更新帖子
     *
     * @param forumUpdateDto
     * @return
     */
    @Override
    public boolean updateForum(ForumUpdateDto forumUpdateDto, User loginUser) {
        //判空
        Forum forum = this.getById(forumUpdateDto.getId());
        ThrowUtils.throwIf(forum == null, ErrorCode.PARAMS_ERROR, "帖子不存在");
        //判断是否为管理员
        if (!forum.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改该帖子");
        }
        //更新帖子
        BeanUtils.copyProperties(forumUpdateDto, forum);
        Boolean b = redissonLock.lockExecute(String.valueOf(forumUpdateDto.getId()), () -> this.updateById(forum));
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR, "帖子更新失败");
        return true;
    }

    @Override
    public QueryWrapper<Forum> getQueryWrapper(ForumQueryDto forumQueryDto) {
        ThrowUtils.throwIf(forumQueryDto == null, ErrorCode.PARAMS_ERROR, "参数错误");
        Long id = forumQueryDto.getId();
        String title = forumQueryDto.getTitle();
        String content = forumQueryDto.getContent();
        Long userId = forumQueryDto.getUserId();
        String category = forumQueryDto.getCategory();
        Integer likeCount = forumQueryDto.getLikeCount();
        Integer viewCount = forumQueryDto.getViewCount();
        Integer shareCount = forumQueryDto.getShareCount();
        Integer commentCount = forumQueryDto.getCommentCount();
        Integer reviewStatus = forumQueryDto.getReviewStatus();
        Long reviewerId = forumQueryDto.getReviewerId();
        String sortField = forumQueryDto.getSortField();
        String sortOrder = forumQueryDto.getSortOrder();
        String reviewMessage = forumQueryDto.getReviewMessage();
        QueryWrapper<Forum> qw = new QueryWrapper<>();
        qw.eq(id != null, "id", id);
        qw.like(StringUtil.isNotBlank(title), "title", title);
        qw.eq(content != null, "content", content);
        qw.eq(ObjectUtil.isNotNull(userId), "userId", userId);
        qw.eq(StringUtil.isNotBlank(category), "category", category);
        qw.eq(StringUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        qw.eq(ObjectUtil.isNotNull(likeCount), "likeCount", likeCount);
        qw.eq(ObjectUtil.isNotNull(viewCount), "viewCount", viewCount);
        qw.eq(ObjectUtil.isNotNull(shareCount), "shareCount", shareCount);
        qw.eq(ObjectUtil.isNotNull(commentCount), "commentCount", commentCount);
        qw.eq(ObjectUtil.isNotNull(reviewStatus), "reviewStatus", reviewStatus);
        qw.eq(ObjectUtil.isNotNull(reviewerId), "reviewerId", reviewerId);
        qw.orderBy(StringUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return qw;
    }

    @Override
    public Page<ForumVO> listForumVO(Page<Forum> page) {
        List<Forum> records = page.getRecords();
        Page<ForumVO> pageVO = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());

        if (records.isEmpty()) {
            return pageVO;
        }
        //转化成vo类
        List<ForumVO> listForumVO = records.stream()
                .map(ForumVO::objToVo)
                .collect(Collectors.toList());
        //获取用户id
        Set<Long> listUserId = records.stream()
                .map(Forum::getUserId)
                .collect(Collectors.toSet());
        //让用户的id和用户一一对应，组成map集合
        Map<Long, List<User>> userMap = userService.listByIds(listUserId)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
        //将用户信息设置到帖子中
        listForumVO.forEach(forumVO -> {
            Long userId = forumVO.getUserId();
            User user = null;
            if (userMap.containsKey(userId)) {
                user = userMap.get(userId).get(0);
            }
            forumVO.setUserVO(userService.getUserVo(user));
        });
        pageVO.setRecords(listForumVO);

        return pageVO;
    }

    /**
     * 获取帖子VO
     *
     * @param id
     * @return
     */
    @Override
    public ForumVO getForumVO(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "参数错误");
        Forum forum = this.getById(id);
        ThrowUtils.throwIf(forum == null, ErrorCode.PARAMS_ERROR, "帖子不存在");
        //浏览缓存
        forumCache.addForumViewCache(id);
        ForumVO forumVO = ForumVO.objToVo(forum);
        User user = userService.getById(forum.getUserId());
        forumVO.setUserVO(userService.getUserVo(user));
        return forumVO;
    }

    /**
     * 审核帖子
     *
     * @param forumReviewDto
     * @param loginUser
     * @return
     */
    @Override
    public boolean reviewForum(ForumReviewDto forumReviewDto, User loginUser) {
        Long id = forumReviewDto.getId();
        Integer reviewStatus = forumReviewDto.getReviewStatus();
        ReviewStatusEnum reviewStatusValue = ReviewStatusEnum.getReviewStatusValue(reviewStatus);
        //如果传递的为待审核，不可重复审核
        if (reviewStatusValue == null || ReviewStatusEnum.REVIEWING.equals(reviewStatusValue)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "待审核，不可重复审核");
        }
        Forum oldForum = this.getById(id);
        ThrowUtils.throwIf(oldForum == null, ErrorCode.PARAMS_ERROR, "帖子不存在");
        //审核状态和 帖子的审核状态一致
        if (oldForum.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "不可重复审核");
        }
        Forum forum = new Forum();
        BeanUtils.copyProperties(forumReviewDto, forum);
        forum.setReviewerId(loginUser.getId());
        if (userService.isAdmin(loginUser) && forum.getReviewMessage() == null) {
            forum.setReviewMessage("管理员自动通过");
        }
        boolean update = this.updateById(forum);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "审核失败");
        return true;
    }

}





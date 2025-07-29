package com.ph.phpictureback.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
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
import com.ph.phpictureback.mapper.ForumMapper;
import com.ph.phpictureback.model.dto.follow.FollowQueryDto;
import com.ph.phpictureback.model.dto.forum.ForumAddDto;
import com.ph.phpictureback.model.dto.forum.ForumQueryDto;
import com.ph.phpictureback.model.dto.forum.ForumReviewDto;
import com.ph.phpictureback.model.dto.forum.ForumUpdateDto;
import com.ph.phpictureback.model.dto.forumFile.ForumFileAddDto;
import com.ph.phpictureback.model.dto.forumFile.ForumFileQueryDto;
import com.ph.phpictureback.model.dto.picture.UploadPictureDto;
import com.ph.phpictureback.model.entry.Forum;
import com.ph.phpictureback.model.entry.ForumFile;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.ReviewStatusEnum;
import com.ph.phpictureback.model.vo.FollowVO;
import com.ph.phpictureback.model.vo.ForumVO;
import com.ph.phpictureback.service.FollowService;
import com.ph.phpictureback.service.ForumFileService;
import com.ph.phpictureback.service.ForumService;
import com.ph.phpictureback.service.UserService;
import jodd.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

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

    @Resource
    private ForumFileService forumFileService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private FollowService followService;

    /**
     * 添加帖子
     *
     * @param forumAddDto
     * @param loginUser
     * @return
     */
    @Override
    public boolean addForum(ForumAddDto forumAddDto, User loginUser) {
        Forum forum = new Forum();
        BeanUtils.copyProperties(forumAddDto, forum);
        //设置创建人
        Long userId = loginUser.getId();
        forum.setUserId(userId);
        vaildForum(forum);
        //保存帖子
        Boolean b = redissonLock.lockExecute(String.valueOf(userId), () -> this.save(forum));
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR, "帖子创建失败");
        //得到这个帖子的所有 图片id
        List<Long> idList = forumAddDto.getListForumFileId();
        if (CollectionUtils.isEmpty(idList)) {
            return true;
        }
        //帖子文件表的 更新对应的帖子id
        idList.forEach(id -> {
            ForumFile forumFile = new ForumFile();
            forumFile.setForumId(forum.getId());
            forumFile.setId(id);
            boolean update = forumFileService.updateById(forumFile);
            ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "帖子文件更新失败");
        });
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
        //启动事务
        transactionTemplate.execute((status) -> {
            //删除帖子
            Boolean b = redissonLock.lockExecute(String.valueOf(id), () -> this.removeById(id));
            ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR, "帖子删除失败");
            // 删除帖子带的图片
            QueryWrapper<ForumFile> qw = new QueryWrapper<>();
            qw.eq("forumId", id);
            boolean remove = forumFileService.remove(qw);
            ThrowUtils.throwIf(!remove, ErrorCode.SYSTEM_ERROR, "帖子文件删除失败");
            return true;
        });

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
        //验证
        vaildForum(forum);
        Boolean b = redissonLock.lockExecute(String.valueOf(forumUpdateDto.getId()), () -> this.updateById(forum));
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR, "帖子更新失败");
        //得到这个帖子的所有 图片id
        List<Long> idList = forumUpdateDto.getListForumFileId();
        if (CollectionUtils.isEmpty(idList)) {
            return true;
        }
        //帖子文件表的 更新对应的帖子id
        idList.forEach(id -> {
            ForumFile forumFile = new ForumFile();
            forumFile.setForumId(forum.getId());
            forumFile.setId(id);
            boolean update = forumFileService.updateById(forumFile);
            ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "帖子文件更新失败");
        });
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
            ForumFileQueryDto query = new ForumFileQueryDto();
            query.setForumId(forumVO.getId());
            List<ForumFile> forumFileList = this.queryForumFile(query);
            forumVO.setForumFile(forumFileList);
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
        //得到帖子的文件
        ForumFileQueryDto query = new ForumFileQueryDto();
        query.setForumId(id);
        List<ForumFile> forumFileList = this.queryForumFile(query);
        List<ForumFile> sortList = forumFileList.stream().sorted(Comparator.comparingInt(ForumFile::getSort)).collect(Collectors.toList());
        forumVO.setForumFile(sortList);
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

    /**
     * 添加帖子文件
     *
     * @param forumFileAddDto
     * @return
     */
    @Override
    public ForumFile addForumFile(Object inputSource, ForumFileAddDto forumFileAddDto,User loginUser) {
        ForumFile forumFile = new ForumFile();
        BeanUtils.copyProperties(forumFileAddDto, forumFile);
        //有id就是更新
        if (forumFileAddDto.getId() != null) {
            boolean byId = forumFileService.lambdaQuery()
                    .eq(ForumFile::getId, forumFileAddDto.getId())
                    .exists();
            ThrowUtils.throwIf(!byId, ErrorCode.PARAMS_ERROR, "文件不存在");
        }
        //上传文件
        //存放地址
        String uploadPath = String.format("/forum/%s", loginUser.getId());
        //判断是文件上传还是url上传
        DownloadFileTemplate downloadFileTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            downloadFileTemplate = urlPictureUpload;
        }
        UploadPictureDto uploadPicture = downloadFileTemplate.uploadPicture(inputSource, uploadPath);
        forumFile.setPicUrl(uploadPicture.getUrl());
        forumFile.setThumbnailUrl(uploadPicture.getThumbnailUrl());
        forumFile.setSize(uploadPicture.getPicSize());
        boolean update = forumFileService.saveOrUpdate(forumFile);
        ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "上传失败");
        return forumFile;
    }

    /**
     * 查询帖子文件
     *
     * @param forumFileQueryDto
     * @return
     */
    @Override
    public List<ForumFile> queryForumFile(ForumFileQueryDto forumFileQueryDto) {
        QueryWrapper<ForumFile> qw = new QueryWrapper<>();
        qw.eq("forumId", forumFileQueryDto.getForumId());
        qw.eq(ObjectUtil.isNotEmpty(forumFileQueryDto.getType()), "type", forumFileQueryDto.getType());
        qw.eq(ObjectUtil.isNotEmpty(forumFileQueryDto.getId()), "id", forumFileQueryDto.getId());
        List<ForumFile> list = forumFileService.list(qw);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list;
    }

    @Override
    public Page<ForumVO> getFollowFor(ForumQueryDto forumQueryDto, User loginUser) {
        List<Long> userIdList = followService.getListFollow(loginUser);
        int current = forumQueryDto.getCurrent();
        int pageSize = forumQueryDto.getPageSize();
        if(CollUtil.isEmpty(userIdList)){
            return new Page<>();
        }
        Page<Forum> page = this.page(new Page<>(current, pageSize),
                this.getQueryWrapper(forumQueryDto).in("userId", userIdList));
        return this.listForumVO(page);
    }


    /**
     * 帖子校验
     * @param forum
     */
    private static void vaildForum(Forum forum) {
        //验证帖子信息
        if (StringUtil.isBlank(forum.getTitle())   || StringUtil.isBlank(forum.getContent())  || StringUtil.isBlank(forum.getCategory())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子信息不完整");
        }
        if (forum.getTitle().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
    }

}





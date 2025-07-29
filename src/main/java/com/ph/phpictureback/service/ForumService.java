package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.model.dto.follow.FollowQueryDto;
import com.ph.phpictureback.model.dto.forum.ForumAddDto;
import com.ph.phpictureback.model.dto.forum.ForumQueryDto;
import com.ph.phpictureback.model.dto.forum.ForumReviewDto;
import com.ph.phpictureback.model.dto.forum.ForumUpdateDto;
import com.ph.phpictureback.model.dto.forumFile.ForumFileAddDto;
import com.ph.phpictureback.model.dto.forumFile.ForumFileQueryDto;
import com.ph.phpictureback.model.entry.Forum;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.model.entry.ForumFile;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.FollowVO;
import com.ph.phpictureback.model.vo.ForumVO;
import org.springframework.web.bind.annotation.RequestPart;

import java.util.List;

/**
* @author 杨志亮
* @description 针对表【forum(论坛表)】的数据库操作Service
* @createDate 2025-06-23 11:50:19
*/
public interface ForumService extends IService<Forum> {
    /**
     * 添加帖子
     * @param forumAddDto
     * @param loginUser
     * @return
     */
    boolean addForum(ForumAddDto forumAddDto, User loginUser);

    /**
     * 删除帖子
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteForum(long id, User loginUser);

    /**
     * 更新帖子
     * @param forumUpdateDto
     * @return
     */
    boolean updateForum(ForumUpdateDto forumUpdateDto,User loginUser);

    QueryWrapper<Forum> getQueryWrapper(ForumQueryDto forumQueryDto);

    Page<ForumVO> listForumVO(Page<Forum> page);

    /**
     * 获取帖子VO
     * @param id
     * @return
     */
    ForumVO getForumVO(Long id);

    /**
     * 审核帖子
     * @param forumReviewDto
     * @param loginUser
     * @return
     */
    boolean reviewForum(ForumReviewDto forumReviewDto, User loginUser);

    /**
     * 添加帖子文件

     * @param inputSource 输入源
     * @param forumFileAddDto 帖子文件添加DTO
     * @return 添加的帖子文件
     */
    ForumFile addForumFile(Object inputSource, ForumFileAddDto forumFileAddDto,User loginUser);

    /**
     * 查询帖子文件
     * @param forumFileQueryDto
     * @return
     */
    List<ForumFile> queryForumFile(ForumFileQueryDto forumFileQueryDto);

    /**
     * 查询我关注的帖子
     * @param forumQueryDto
     * @param loginUser
     * @return
     */
    Page<ForumVO> getFollowFor(ForumQueryDto forumQueryDto, User loginUser);
}

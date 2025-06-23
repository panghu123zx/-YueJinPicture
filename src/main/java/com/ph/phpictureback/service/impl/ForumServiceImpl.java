package com.ph.phpictureback.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.DownloadFileTemplate;
import com.ph.phpictureback.manager.RedissonLock;
import com.ph.phpictureback.manager.fileUpload.FilePictureUpload;
import com.ph.phpictureback.manager.fileUpload.UrlPictureUpload;
import com.ph.phpictureback.model.dto.forum.ForumAddDto;
import com.ph.phpictureback.model.dto.forum.ForumUpdateDto;
import com.ph.phpictureback.model.dto.picture.UploadPictureDto;
import com.ph.phpictureback.model.entry.Forum;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.service.ForumService;
import com.ph.phpictureback.mapper.ForumMapper;
import com.ph.phpictureback.service.UserService;
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
    implements ForumService{
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private RedissonLock redissonLock;
    @Resource
    private UserService userService;

    /**
     * 添加帖子
     * @param forumAddDto
     * @param loginUser
     * @return
     */
    @Override
    public boolean addForum(Object inputSource,ForumAddDto forumAddDto, User loginUser) {
        Forum forum = new Forum();
        BeanUtils.copyProperties(forumAddDto, forum);
        //设置创建人
        Long userId = loginUser.getId();
        forum.setUserId(userId);
        //验证帖子信息
        if(forum.getTitle() == null || forum.getContent() == null || forum.getCategory() == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子信息不完整");
        }
        if(forum.getTitle().length() > 50){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        //论坛可以不带有图片
        if(inputSource != null){
            //上传图片
            DownloadFileTemplate pictureUpload = filePictureUpload;
            //判断是否为文url上传
            if(inputSource instanceof  String){
                pictureUpload = urlPictureUpload;
            }
            //图片存放地址
            String uploadPath=String.format("/forum/%s", userId);
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
        if(!forum.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
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
     * @param forumUpdateDto
     * @return
     */
    @Override
    public boolean updateForum(ForumUpdateDto forumUpdateDto) {
        //判空
        Forum forum = this.getById(forumUpdateDto.getId());
        ThrowUtils.throwIf(forum == null, ErrorCode.PARAMS_ERROR, "帖子不存在");
        //更新帖子
        BeanUtils.copyProperties(forumUpdateDto, forum);
        Boolean b = redissonLock.lockExecute(String.valueOf(forumUpdateDto.getId()), () -> this.updateById(forum));
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR, "帖子更新失败");
        return true;
    }

}





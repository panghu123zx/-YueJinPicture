package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.model.dto.likeMessage.LikeMessageAddDto;
import com.ph.phpictureback.model.dto.likeMessage.LikeMessageQueryDto;
import com.ph.phpictureback.model.dto.likeMessage.LikeMessageReadDto;
import com.ph.phpictureback.model.entry.LikeMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.LikeMessageVO;

/**
* @author 杨志亮
* @description 针对表【like_message(点赞/分享消息)】的数据库操作Service
* @createDate 2025-07-06 16:24:41
*/
public interface LikeMessageService extends IService<LikeMessage> {

    /**
     * 发送消息
     * @param likeMessageAddDto
     * @param loginUser
     * @return
     */
    Long addLikeMessage(LikeMessageAddDto likeMessageAddDto, User loginUser);

    /**
     * 读取消息
     * @param likeMessageReadDto
     * @return
     */
    boolean readMessage(LikeMessageReadDto likeMessageReadDto);

    QueryWrapper<LikeMessage> getQueryWrapper(LikeMessageQueryDto likeMessageQueryDto);

    /**
     * 获取消息列表
     * @param page
     * @return
     */
    Page<LikeMessageVO> listLikeMessageVO(Page<LikeMessage> page,User loginUser);
}

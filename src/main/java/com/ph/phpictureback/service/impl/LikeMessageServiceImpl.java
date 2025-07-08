package com.ph.phpictureback.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.likeMessage.LikeMessageAddDto;
import com.ph.phpictureback.model.dto.likeMessage.LikeMessageQueryDto;
import com.ph.phpictureback.model.dto.likeMessage.LikeMessageReadDto;
import com.ph.phpictureback.model.entry.LikeMessage;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.enums.ForumPictureTypeEnum;
import com.ph.phpictureback.model.vo.ForumVO;
import com.ph.phpictureback.model.vo.LikeMessageVO;
import com.ph.phpictureback.model.vo.PictureVO;
import com.ph.phpictureback.model.vo.UserVO;
import com.ph.phpictureback.service.ForumService;
import com.ph.phpictureback.service.LikeMessageService;
import com.ph.phpictureback.mapper.LikeMessageMapper;
import com.ph.phpictureback.service.PictureService;
import com.ph.phpictureback.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 杨志亮
 * @description 针对表【like_message(点赞/分享消息)】的数据库操作Service实现
 * @createDate 2025-07-06 16:24:41
 */
@Service
public class LikeMessageServiceImpl extends ServiceImpl<LikeMessageMapper, LikeMessage>
        implements LikeMessageService {
    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private ForumService forumService;

    /**
     * 发送消息
     *
     * @param likeMessageAddDto
     * @param loginUser
     * @return
     */
    @Override
    public Long addLikeMessage(LikeMessageAddDto likeMessageAddDto, User loginUser) {
        Long receiverId = likeMessageAddDto.getReceiverId();
        //判断接收者是否存在
        boolean exists = userService.lambdaQuery()
                .eq(User::getId, receiverId)
                .exists();
        ThrowUtils.throwIf(!exists, ErrorCode.PARAMS_ERROR, "用户不存在");
        LikeMessage likeMessage = new LikeMessage();
        BeanUtils.copyProperties(likeMessageAddDto, likeMessage);
        //消息发送者必须是当前登入用户
        likeMessage.setSendId(loginUser.getId());
        boolean save = this.save(likeMessage);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "消息发送失败");
        return likeMessage.getId();
    }

    /**
     * 读取消息
     *
     * @param likeMessageReadDto
     * @return
     */
    @Override
    public boolean readMessage(LikeMessageReadDto likeMessageReadDto) {
        Long id = likeMessageReadDto.getId();
        LikeMessage exist = this.getById(id);
        ThrowUtils.throwIf(exist == null, ErrorCode.PARAMS_ERROR, "消息不存在");
        LikeMessage likeMessage = new LikeMessage();
        likeMessage.setId(id);
        likeMessage.setIsRead(1);
        boolean update = this.updateById(likeMessage);
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "消息读取失败");
        return true;
    }

    @Override
    public QueryWrapper<LikeMessage> getQueryWrapper(LikeMessageQueryDto likeMessageQueryDto) {
        ThrowUtils.throwIf(likeMessageQueryDto == null, ErrorCode.PARAMS_ERROR, "参数为空");
        QueryWrapper<LikeMessage> qw = new QueryWrapper<>();

        Long id = likeMessageQueryDto.getId();
        Long sendId = likeMessageQueryDto.getSendId();
        Long receiverId = likeMessageQueryDto.getReceiverId();
        Integer isRead = likeMessageQueryDto.getIsRead();
        Integer actionType = likeMessageQueryDto.getActionType();
        Integer targetType = likeMessageQueryDto.getTargetType();
        Long targetId = likeMessageQueryDto.getTargetId();
        String sortField = likeMessageQueryDto.getSortField();
        String sortOrder = likeMessageQueryDto.getSortOrder();

        qw.eq(ObjectUtil.isNotNull(id), "id", id);
        qw.eq(ObjectUtil.isNotNull(sendId), "sendId", sendId);
        qw.eq(ObjectUtil.isNotNull(receiverId), "receiverId", receiverId);
        qw.eq(ObjectUtil.isNotNull(targetId), "targetId", targetId);
        qw.eq(ObjectUtil.isNotNull(isRead), "isRead", isRead);
        qw.eq(ObjectUtil.isNotNull(actionType), "actionType", actionType);
        qw.eq(ObjectUtil.isNotNull(targetType), "targetType", targetType);

        qw.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);


        return qw;
    }

    /**
     * 获取消息列表
     *
     * @param page
     * @return
     */
    @Override
    public Page<LikeMessageVO> listLikeMessageVO(Page<LikeMessage> page,User loginUser) {
        List<LikeMessage> records = page.getRecords();
        //设置新值
        Page<LikeMessageVO> pageVo = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(records)) {
            return pageVo;
        }
        //vo类的转化
        List<LikeMessageVO> messageVOList = records.stream()
                .map(LikeMessageVO::objToVo)
                .collect(Collectors.toList());

        //获取发送者id集合
        Set<Long> sendIdList = records.stream()
                .map(LikeMessage::getSendId)
                .collect(Collectors.toSet());
        //获取到用户集合
        Map<Long, List<User>> userMap = userService.listByIds(sendIdList)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        messageVOList.forEach(likeMessageVO -> {
            //设置发送者的用户
            Long sendId = likeMessageVO.getSendId();
            if (userMap.containsKey(sendId)) {
                User user = userMap.get(sendId).get(0);
                UserVO userVO = UserVO.objToVo(user);
                likeMessageVO.setUserVO(userVO);
            }
            //设置目标
            Integer targetType = likeMessageVO.getTargetType();
            Long targetId = likeMessageVO.getTargetId();
            if (targetType.equals(ForumPictureTypeEnum.PICTURE.getValue())) {
                PictureVO pictureVo = pictureService.getPictureVo(targetId, loginUser);
                likeMessageVO.setPictureVO(pictureVo);
            } else {
                ForumVO forumVO = forumService.getForumVO(targetId);
                likeMessageVO.setForumVO(forumVO);
            }
        });

        pageVo.setRecords(messageVOList);
        return pageVo;
    }
}





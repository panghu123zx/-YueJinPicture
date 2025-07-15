package com.ph.phpictureback.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.chatPrompt.ChatPromptAddDto;
import com.ph.phpictureback.model.dto.chatPrompt.ChatPromptQueryDto;
import com.ph.phpictureback.model.dto.chatPrompt.ChatPromptUpdateDto;
import com.ph.phpictureback.model.entry.ChatPrompt;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.ChatPromptVO;
import com.ph.phpictureback.service.ChatPromptService;
import com.ph.phpictureback.mapper.ChatPromptMapper;
import com.ph.phpictureback.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author 杨志亮
 * @description 针对表【chat_prompt(消息提示表)】的数据库操作Service实现
 * @createDate 2025-07-13 17:12:19
 */
@Service
public class ChatPromptServiceImpl extends ServiceImpl<ChatPromptMapper, ChatPrompt>
        implements ChatPromptService {

    @Resource
    private UserService userService;

    /**
     * 新增消息提示
     *
     * @param chatPromptAddDto
     * @return
     */
    @Override
    public Long addChatPrompt(ChatPromptAddDto chatPromptAddDto) {
        ThrowUtils.throwIf(chatPromptAddDto == null, ErrorCode.PARAMS_ERROR, "消息提示不能为空");
        Long userId = chatPromptAddDto.getUserId();
        Long targetId = chatPromptAddDto.getTargetId();
        boolean exists = this.lambdaQuery()
                .eq(ChatPrompt::getUserId, userId)
                .eq(ChatPrompt::getTargetId, targetId)
                .exists();
        ThrowUtils.throwIf(exists, ErrorCode.PARAMS_ERROR, "消息提示已存在");

        ChatPrompt chatPrompt = new ChatPrompt();
        BeanUtils.copyProperties(chatPromptAddDto, chatPrompt);
        boolean save = this.save(chatPrompt);
        ThrowUtils.throwIf(!save, ErrorCode.PARAMS_ERROR, "消息提示新增失败");
        return chatPrompt.getId();
    }

    /**
     * 修改消息提示
     *
     * @param chatUpdateAddDto
     * @return
     */
    @Override
    public Boolean updateChatPrompt(ChatPromptUpdateDto chatUpdateAddDto) {
        Long id = chatUpdateAddDto.getId();
        Long userId = chatUpdateAddDto.getUserId();
        Long targetId = chatUpdateAddDto.getTargetId();
        //用户点击关注的时候id为null
        if (id == null) {
            ChatPrompt one = this.lambdaQuery()
                    .eq(ChatPrompt::getUserId, userId)
                    .eq(ChatPrompt::getTargetId, targetId)
                    .one();
            id = one.getId();
        }
        ChatPrompt chatPrompt = new ChatPrompt();
        BeanUtils.copyProperties(chatUpdateAddDto, chatPrompt);
        chatPrompt.setId(id);
        boolean update = this.updateById(chatPrompt);
        ThrowUtils.throwIf(!update, ErrorCode.PARAMS_ERROR, "消息提示修改失败");
        return true;
    }

    @Override
    public QueryWrapper<ChatPrompt> getQueryWrapper(ChatPromptQueryDto chatPromptQueryDto) {
        ThrowUtils.throwIf(chatPromptQueryDto == null, ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        QueryWrapper<ChatPrompt> qw = new QueryWrapper<>();

        Long id = chatPromptQueryDto.getId();
        Long userId = chatPromptQueryDto.getUserId();
        Long targetId = chatPromptQueryDto.getTargetId();
        String title = chatPromptQueryDto.getTitle();
        String receiveTitle = chatPromptQueryDto.getReceiveTitle();
        Integer chatType = chatPromptQueryDto.getChatType();
        Integer unreadCount = chatPromptQueryDto.getUnreadCount();
        String lastMessage = chatPromptQueryDto.getLastMessage();
        String sortField = chatPromptQueryDto.getSortField();
        String sortOrder = chatPromptQueryDto.getSortOrder();
        Integer isQuery = chatPromptQueryDto.getIsQuery();

        qw.eq(ObjectUtil.isNotNull(id), "id", id);
        qw.like(StrUtil.isNotEmpty(title), "title", title);
        qw.like(StrUtil.isNotEmpty(receiveTitle), "receiveTitle", receiveTitle);
        qw.eq(ObjectUtil.isNotNull(chatType), "chatType", chatType);
        qw.eq(ObjectUtil.isNotNull(unreadCount), "unreadCount", unreadCount);
        qw.eq(StrUtil.isNotEmpty(lastMessage), "lastMessage", lastMessage);

        if (isQuery == 0) {
            qw.and(qw1 ->
                    qw1.eq("userId", userId)
                            .or(sub -> sub.eq("targetId", userId))

            );
        } else {
            qw.eq(ObjectUtil.isNotNull(userId), "userId", userId);
            qw.eq(ObjectUtil.isNotNull(targetId), "targetId", targetId);
        }

        qw.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return qw;
    }

    /**
     * VO类
     *
     * @param page
     * @param loginUser
     * @return
     */
    @Override
    public Page<ChatPromptVO> listVO(Page<ChatPrompt> page, User loginUser) {
        Long id = loginUser.getId();
        List<ChatPrompt> records = page.getRecords();
        Page<ChatPromptVO> pageVO = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(records)) {
            return pageVO;
        }
        //转换为VO类
        List<ChatPromptVO> chatPromptVOList = records.stream()
                .map(ChatPromptVO::objToVo)
                .collect(Collectors.toList());


        Set<Long> targetIdSet = records.stream()
                .map(chatPrompt -> {
                    Long userId = chatPrompt.getUserId();
                    Long targetId = chatPrompt.getTargetId();
                    if (!userId.equals(id)) {
                        return userId;
                    } else {
                        return targetId;
                    }
                })
                .collect(Collectors.toSet());

        Map<Long, List<User>> targetUser = userService.listByIds(targetIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        chatPromptVOList.forEach(chatPromptVO -> {
            Long targetId = chatPromptVO.getTargetId();
            Long userId = chatPromptVO.getUserId();
            if (targetId.equals(id)) {
                if (targetUser.containsKey(userId)) {
                    User user = targetUser.get(userId).get(0);
                    chatPromptVO.setTargetUserVO(userService.getUserVo(user));
                    chatPromptVO.setIsReceive(1);
                }
            }else{
                if (targetUser.containsKey(targetId)) {
                    User user = targetUser.get(targetId).get(0);
                    chatPromptVO.setTargetUserVO(userService.getUserVo(user));
                }
            }

        });

        pageVO.setRecords(chatPromptVOList);
        return pageVO;
    }
}





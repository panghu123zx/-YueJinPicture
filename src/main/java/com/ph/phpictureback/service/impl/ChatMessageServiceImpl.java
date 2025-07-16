package com.ph.phpictureback.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.manager.websocket.model.ChatResponseMessage;
import com.ph.phpictureback.model.entry.AudioFile;
import com.ph.phpictureback.model.entry.ChatMessage;
import com.ph.phpictureback.mapper.ChatMessageMapper;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.ChatMessageVO;
import com.ph.phpictureback.service.AudioFileService;
import com.ph.phpictureback.service.ChatMessageService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 杨志亮
* @description 针对表【chat_message(用户聊天表)】的数据库操作Service实现
* @createDate 2025-07-13 17:12:12
*/
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
    implements ChatMessageService {

    @Resource
    private AudioFileService audioFileService;

    /**
     * 获取历史消息
     * @param id
     * @param current
     * @param pageSize
     * @return
     */
    @Override
    public Page<ChatMessageVO> getHistoryMessages(Long id, Long current, Long pageSize) {
        //todo 使用缓存
        QueryWrapper<ChatMessage> qw = new QueryWrapper<>();
        qw.eq("chatPromptId",id).orderByAsc("createTime");
        Page<ChatMessage> page = this.page(new Page<>(current, pageSize), qw);
        Page<ChatMessageVO> pageVO = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ChatMessage> records = page.getRecords();
        if (CollUtil.isEmpty(records)){
            return pageVO;
        }
        List<ChatMessageVO> chatMessageVOList = records.stream()
                .map(ChatMessageVO::objToVo)
                .collect(Collectors.toList());

        Set<Long> targetIdList = records.stream()
                .map(ChatMessage::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        //如果没有文件信息
        if (CollUtil.isEmpty(targetIdList)){
            pageVO.setRecords(chatMessageVOList);
        }else{
            Map<Long, List<AudioFile>> fileMap = audioFileService.listByIds(targetIdList)
                    .stream()
                    .collect(Collectors.groupingBy(AudioFile::getId));

            chatMessageVOList.forEach(chatMessageVO -> {
                Long targetId = chatMessageVO.getTargetId();
                if(targetId!=null){
                    if(fileMap.containsKey(targetId)){
                        AudioFile audioFile = fileMap.get(targetId).get(0);
                        chatMessageVO.setUrl(audioFile.getFileUrl());
                    }
                }
            });
            pageVO.setRecords(chatMessageVOList);
        }
        return pageVO;
    }
}





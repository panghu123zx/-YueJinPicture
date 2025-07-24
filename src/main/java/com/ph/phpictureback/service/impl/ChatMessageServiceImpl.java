package com.ph.phpictureback.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.entry.AudioFile;
import com.ph.phpictureback.model.entry.ChatMessage;
import com.ph.phpictureback.mapper.ChatMessageMapper;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.ChatMessageVO;
import com.ph.phpictureback.service.AudioFileService;
import com.ph.phpictureback.service.ChatMessageService;
import com.ph.phpictureback.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    /**
     * caffeine  本地缓存的设置
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .maximumSize(10000L)
            .expireAfterWrite(5L, TimeUnit.MINUTES)
            .build();

    /**
     * 获取历史消息
     *
     * @param id
     * @param current
     * @param pageSize
     * @return
     */
    @Override
    public Page<ChatMessageVO> getHistoryMessages(Long id, Long current, Long pageSize) {
        //使用缓存
        String redisKey = String.format("Chat:Message:chatId:%d:%d", id, pageSize);
        String caffeineValue = LOCAL_CACHE.getIfPresent(redisKey);
        //caffeine存在直接返回
        if (StrUtil.isNotBlank(caffeineValue)) {
            Page<ChatMessageVO> pageCaff = JSONUtil.toBean(caffeineValue, Page.class);
            return pageCaff;
        }
        //不存在，判断redis中有吗
        ValueOperations<String, String> redisVal = stringRedisTemplate.opsForValue();
        String s = redisVal.get(redisKey);
        if (StrUtil.isNotBlank(s)) {
            //存在，写入caffeine中，然后返回
            LOCAL_CACHE.put(redisKey, s);
            Page<ChatMessageVO> reidsPage = JSONUtil.toBean(s, Page.class);
            return reidsPage;
        }


        QueryWrapper<ChatMessage> qw = new QueryWrapper<>();
        qw.eq("chatPromptId", id).orderByDesc("createTime");
        Page<ChatMessage> page = this.page(new Page<>(current, pageSize), qw);
        Page<ChatMessageVO> pageVO = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ChatMessage> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return pageVO;
        }
        List<ChatMessageVO> chatMessageVOList = records.stream()
                .map(ChatMessageVO::objToVo)
                .collect(Collectors.toList());

        Map<Long, List<ChatMessage>> chatMessageMap = records.stream()
                .collect(Collectors.groupingBy(ChatMessage::getId));

        Set<Long> targetIdList = records.stream()
                .map(ChatMessage::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        //如果没有文件信息
        if (CollUtil.isNotEmpty(targetIdList)) {
            Map<Long, List<AudioFile>> fileMap = audioFileService.listByIds(targetIdList)
                    .stream()
                    .collect(Collectors.groupingBy(AudioFile::getId));

            chatMessageVOList.forEach(chatMessageVO -> {
                Long targetId = chatMessageVO.getTargetId();
                //文件数据
                if (targetId != null) {
                    if (fileMap.containsKey(targetId)) {
                        AudioFile audioFile = fileMap.get(targetId).get(0);
                        chatMessageVO.setUrl(audioFile.getFileUrl());
                        chatMessageVO.setContent(audioFile.getTitle());
                    }
                }
            });
        }
        chatMessageVOList.forEach(chatMessageVO -> {
            //如果有回复的消息
            Long replayId = chatMessageVO.getReplayId();
            if (replayId != null) {
                //有需要可以加一个if判断
                ChatMessage chatMessage = chatMessageMap.get(replayId).get(0);
                chatMessageVO.setReplayContent(chatMessage.getContent());
            }
        });
        //方便于前端展示时间
        for (int i = 0; i < records.size() - 1; i++) {
            long timeDiff = records.get(i).getCreateTime().getTime() - records.get(i + 1).getCreateTime().getTime();
            // 时间差小于30分钟
            if (timeDiff < 1000 * 60 * 30) {
                chatMessageVOList.get(i).setCreateTime(null);
            }
        }
        pageVO.setRecords(chatMessageVOList);
        //写入redis和caffeine中
        int timeout = 5 + RandomUtil.randomInt(10);
        String jsonStr = JSONUtil.toJsonStr(pageVO);
        redisVal.set(redisKey, jsonStr, timeout, TimeUnit.MINUTES);
        LOCAL_CACHE.put(redisKey, jsonStr);

        return pageVO;
    }


    /**
     * 用户发送消息之后更新缓存
     *
     * @param id
     * @param chatMessage
     */
    public void updateChatCache(Long id, ChatMessage chatMessage) {
        ChatMessageVO chatMessageVO = ChatMessageVO.objToVo(chatMessage);
        String redisKey = String.format("Chat:Message:chatId:%d:%d", id, 50L);
        try {
            ValueOperations<String, String> redisVal = stringRedisTemplate.opsForValue();
            String redisCache = redisVal.get(redisKey);
            if (StrUtil.isNotBlank(redisCache)) {
                Page<ChatMessageVO> page = JSONUtil.toBean(redisCache, new TypeReference<Page<ChatMessageVO>>() {
                }, true);
                List<ChatMessageVO> records = page.getRecords();
                if (CollUtil.isEmpty(records)) {
                    records = new ArrayList<>();
                }
                //如果发送的是文件数据
                if (chatMessage.getMessageType() != null) {
                    Long targetId = chatMessage.getTargetId();
                    AudioFile byId = audioFileService.getById(targetId);
                    chatMessageVO.setContent(byId.getTitle());
                    chatMessageVO.setUrl(byId.getFileUrl());
                }
                //如果有回复的消息
                if (chatMessage.getReplayId() != null) {
                    ChatMessage byId = this.getById(chatMessage.getReplayId());
                    chatMessageVO.setReplayContent(byId.getContent());
                }
                //添加消息
                records.add(0, chatMessageVO);
                //限制长度
                if (records.size() > 100) {
                    records.subList(0, 100);
                }
                // 重新计算时间
                int i = 1;
                while (i < records.size()) {
                    if (records.get(i).getCreateTime() == null) {
                        i++;
                        continue;
                    }
                    long timeDiff = chatMessage.getCreateTime().getTime() - records.get(i).getCreateTime().getTime();
                    // 时间差小于30分钟
                    if (timeDiff < 1000 * 60 * 30) {
                        records.get(0).setCreateTime(null);
                    }
                    break;
                }

                page.setRecords(records);
                page.setTotal(page.getTotal() + 1);

                //序列回redis中
                String newPage = JSONUtil.toJsonStr(page);
                stringRedisTemplate.opsForValue().set(redisKey, newPage, 5 + RandomUtil.randomInt(5), TimeUnit.MINUTES);
                LOCAL_CACHE.put(redisKey, newPage);
            }
        } catch (Exception e) {
            log.error("更新缓存失败", e);
        }

    }

    /**
     * 撤回消息
     *
     * @param id
     * @param loginUser
     * @return
     */
    @Override
    public boolean backMessage(Long id, User loginUser) {
        ChatMessage chatMessage = this.getById(id);
        ThrowUtils.throwIf(chatMessage == null, ErrorCode.PARAMS_ERROR, "消息不存在");
        Long sendId = chatMessage.getSendId();
        if (!sendId.equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            return false;
        }

        boolean update = this.lambdaUpdate()
                .eq(ChatMessage::getId, id)
                .set(ChatMessage::getIsRecalled, 1)
                .set(ChatMessage::getContent, "该消息已被撤回")
                .update(null);
        ThrowUtils.throwIf(!update, ErrorCode.PARAMS_ERROR, "消息撤回失败");
        //更新缓存
        String redisKey = String.format("Chat:Message:chatId:%d:%d", chatMessage.getChatPromptId(), 50L);
        try {
            ValueOperations<String, String> redisVal = stringRedisTemplate.opsForValue();
            String redisCache = redisVal.get(redisKey);
            Page<ChatMessageVO> page = JSONUtil.toBean(redisCache, new TypeReference<Page<ChatMessageVO>>() {}, true);
            List<ChatMessageVO> records = page.getRecords();
            records.forEach(item -> {
                //更新撤回消息的缓存
                if(item.getId().equals(id)){
                    item.setContent("该消息已被撤回");
                    item.setIsRecalled(1);
                }
            });
            page.setRecords(records);
            //序列回redis中
            String newPage = JSONUtil.toJsonStr(page);
            stringRedisTemplate.opsForValue().set(redisKey, newPage, 5 + RandomUtil.randomInt(5), TimeUnit.MINUTES);
            LOCAL_CACHE.put(redisKey, newPage);
        }catch (Exception e){
            log.error("更新缓存失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"更新缓存失败");
        }
        return true;
    }
}





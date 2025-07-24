package com.ph.phpictureback.manager.ai.aiPicture;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.constant.RabbitMqAiConstant;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.ai.AIManager;
import com.ph.phpictureback.manager.websocket.ChatHandler;
import com.ph.phpictureback.manager.websocket.model.ChatMessageTypeEnum;
import com.ph.phpictureback.manager.websocket.model.ChatResponseMessage;
import com.ph.phpictureback.model.dto.picture.PictureUploadDto;
import com.ph.phpictureback.model.entry.ChatMessage;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.ChatMessageVO;
import com.ph.phpictureback.model.vo.PictureVO;
import com.ph.phpictureback.service.ChatMessageService;
import com.ph.phpictureback.service.PictureService;
import com.ph.phpictureback.service.UserService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;


/**
 * ai消息的消费者
 */
@Slf4j
@Component
public class AiPictureConsumer {

    @Resource
    private AiPicture aiPicture;
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;

    /**
     * 调用ai处理消息
     * @param message
     */
    @SneakyThrows
    @RabbitListener(queues = RabbitMqAiConstant.AI_PICTURE_QUEUE,ackMode = "MANUAL")
    public void receiveAiMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        //消息为空时
        if(StrUtil.isBlank(message)){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"消息为空");
        }
        Long id = Long.valueOf(message);
        Picture picture = pictureService.getById(id);
        if(picture==null){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"消息为空");
        }
        //消息是创建图片的指示
        MultipartFile multipartFile = aiPicture.getAiPicture(message, 640L, 640L);
        PictureUploadDto pictureUploadDto = new PictureUploadDto();
        pictureUploadDto.setPicName(picture.getName());
        pictureUploadDto.setId(picture.getId());
        pictureUploadDto.setSpaceId(picture.getSpaceId());
        Long userId = picture.getUserId();
        User user = userService.getById(userId);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadDto, user);
        if(pictureVO==null){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"消息为空");
        }
        channel.basicAck(deliveryTag,false);
    }
}

package com.ph.phpictureback.manager.ai;

import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;

@Service
@Slf4j
public class AIManager {

    @Resource
    private SparkClient sparkClient;


    /**
     * @param content        内容
     * @return
     */
    public String sendMsgToAI(String content) {
        //todo AI 生成问题的预设
        //发送给AI的内容
        ArrayList<SparkMessage> message = new ArrayList<>();
        message.add(SparkMessage.userContent(content));
        //构造请求
        SparkRequest sparkRequest = SparkRequest.builder()
                .messages(message) //消息列表
                .maxTokens(2048) //Ai回答的Token的最大长度
                .temperature(0.2) //随机性，取值越高得到的相同问题的不同答案越高
                .apiVersion(SparkApiVersion.V3_5) //版本
                .build();

        //同步调用
        SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
        String res = chatResponse.getContent();
        log.info("星火AI 返回的结果: {}", res);
        return res;
    }
}

package com.ph.phpictureback.api.aliyun;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.ph.phpictureback.api.aliyun.model.CreateOutPaintingTaskDto;
import com.ph.phpictureback.api.aliyun.model.CreateOutPaintingTaskVo;
import com.ph.phpictureback.api.aliyun.model.GetOutPaintingTaskVo;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliyunApi {

    @Value("${aliyun.apiKey}")
    private String apiKey;

    //创建任务id
    private static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";
    //获取任务状态
    private static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建扩图任务
     *
     * @param createOutPaintingTaskDto
     * @return
     */
    public CreateOutPaintingTaskVo createOutPaintingTask(CreateOutPaintingTaskDto createOutPaintingTaskDto) {
        ThrowUtils.throwIf(createOutPaintingTaskDto == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        //发送http请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer" + apiKey)
                //开启异步处理
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, "application/json")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskDto));
        try  {
            HttpResponse response = httpRequest.execute();
            //判断响应结果是否成功
            if (!response.isOk()) {
                log.error("发送扩图任务失败");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送扩图任务失败");
            }
            //封装返回类
            CreateOutPaintingTaskVo createOutPaintingTaskVo = JSONUtil.toBean(response.body(), CreateOutPaintingTaskVo.class);
            //得到错误码
            String errorCode = createOutPaintingTaskVo.getCode();
            //判断错误码是否为空
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = createOutPaintingTaskVo.getMessage();
                log.error("发送扩图任务失败,错误码:{},错误信息:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送扩图任务失败");
            }
            return createOutPaintingTaskVo;
        }catch (Exception e){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送扩图任务失败");
        }
    }

    /**
     * 获取扩图任务状态
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskVo getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "参数不能为空");
        try {
            //发送http请求
            HttpResponse response = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                    .header(Header.AUTHORIZATION, "Bearer" + apiKey)
                    .execute();
            if (!response.isOk()) {
                log.error("发送扩图任务失败");
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送扩图任务失败");
            }
            //封装返回类
            return JSONUtil.toBean(response.body(), GetOutPaintingTaskVo.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取扩图任务失败");
        }
    }
}

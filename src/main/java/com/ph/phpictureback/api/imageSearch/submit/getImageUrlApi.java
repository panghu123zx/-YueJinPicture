package com.ph.phpictureback.api.imageSearch.submit;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 发送请求，获取url地址
 */
@Slf4j
public class getImageUrlApi {

    /**
     * 获取图片界面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImageUrl(String imageUrl) {
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        //当前时间戳
        long uptime = System.currentTimeMillis();
        //请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;

        String acstoken = "jmM4zyI8OUixvSuWh0sCy4xWbsttVMZb9qcRTmn6SuNWg0vCO7N0s6Lffec+IY5yuqHujHmCctF9BVCGYGH0H5SH/H3VPFUl4O4CP1jp8GoAzuslb8kkQQ4a21Tebge8yhviopaiK66K6hNKGPlWt78xyyJxTteFdXYLvoO6raqhz2yNv50vk4/41peIwba4lc0hzoxdHxo3OBerHP2rfHwLWdpjcI9xeu2nJlGPgKB42rYYVW50+AJ3tQEBEROlg/UNLNxY+6200B/s6Ryz+n7xUptHFHi4d8Vp8q7mJ26yms+44i8tyiFluaZAr66/+wW/KMzOhqhXCNgckoGPX1SSYwueWZtllIchRdsvCZQ8tFJymKDjCf3yI/Lw1oig9OKZCAEtiLTeKE9/CY+Crp8DHa8Tpvlk2/i825E3LuTF8EQfzjcGpVnR00Lb4/8A";

        try {
            HttpResponse response = HttpRequest.post(url)
                    .form(formData)
                    .timeout(5000)
                    .header("Acs_Token", acstoken)
                    .execute();

            //判断响应状态
            if (HttpStatus.HTTP_OK != response.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            //解析响应
            String body = response.body();
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);
            //处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rowUrl = (String) data.get("url");

            //url解码
            String searchResultUrl = URLUtil.decode(rowUrl, StandardCharsets.UTF_8);
            //url为空时
            if (searchResultUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            return searchResultUrl;
        } catch (Exception e) {
            log.error("获取图片界面地址失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }

    }


}

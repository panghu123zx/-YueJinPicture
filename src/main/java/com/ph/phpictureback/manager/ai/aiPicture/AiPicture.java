package com.ph.phpictureback.manager.ai.aiPicture;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;
import org.springframework.mock.web.MockMultipartFile; // 必须添加此导入
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@Slf4j
public class AiPicture {

    public static final String hostUrl = "https://spark-api.cn-huabei-1.xf-yun.com/v2.1/tti";
    public static final String appid = "72eb1e65"; //这里填写APPID
    public static final String apiSecret = "YzBlM2ZiOGFkM2RlYzc3NThlZWE1OGI3"; //这里填写Secret
    public static final String apiKey = "b984b01dc96a437a82925e798b3badbc"; //这里填写Key
    public static final Gson gson = new Gson();

    public MultipartFile getAiPicture(String content, Long width, Long height)  {
        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
        String json = buildRequestJson(content, width, height);
        // 发起Post请求
        String res = this.doPostJson(authUrl, null, json);
        try  {
            //保存文件
            JsonParse jsonParse = gson.fromJson(res, JsonParse.class);
            byte[] imageBytes = Base64.getDecoder().decode(jsonParse.payload.choices.text.get(0).content);
            String fileName="ai图片.png";
            return new MockMultipartFile(
                    "file",
                    fileName,
                    "image/png",
                    imageBytes
            );
        } catch (Exception e) {
            log.error("保存图片时出错");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 构造请求参数
     * @param prompt
     * @param width
     * @param height
     * @return
     */
    private String buildRequestJson(String prompt, Long width, Long height) {
        // 使用GSON构建JSON请求，避免字符串拼接
        Map<String, Object> request = new HashMap<>();

        // Header部分
        Map<String, String> header = new HashMap<>();
        header.put("app_id", appid);
        header.put("uid", UUID.randomUUID().toString().substring(0, 15));

        // Parameter部分
        Map<String, Object> parameter = new HashMap<>();
        Map<String, Object> chat = new HashMap<>();
        chat.put("domain", "s291394db");
        chat.put("temperature", 0.5);
        chat.put("max_tokens", 4096);
        chat.put("width", width);
        chat.put("height", height);
        parameter.put("chat", chat);

        // Payload部分
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> message = new HashMap<>();

        List<Map<String, String>> textList = new ArrayList<>();
        Map<String, String> text = new HashMap<>();
        text.put("role", "user");
        text.put("content", prompt);
        textList.add(text);

        message.put("text", textList);
        payload.put("message", message);

        // 组装完整请求
        request.put("header", header);
        request.put("parameter", parameter);
        request.put("payload", payload);

        return gson.toJson(request);
    }

    // 鉴权方法
    public String getAuthUrl(String hostUrl, String apiKey, String apiSecret) {
        HttpUrl httpUrl = null;
        try {
            URL url = new URL(hostUrl);
            // 时间
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());
            // 拼接
            String preStr = "host: " + url.getHost() + "\n" + "date: " + date + "\n" + "POST " + url.getPath() + " HTTP/1.1";
            // SHA256加密
            Mac mac = Mac.getInstance("hmacsha256");
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
            // Base64加密
            String sha = Base64.getEncoder().encodeToString(hexDigits);
            // 拼接
            String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                    apiKey, "hmac-sha256", "host date request-line", sha);
            // 拼接地址
            httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder().//
                    addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8))).//
                    addQueryParameter("date", date).//
                    addQueryParameter("host", url.getHost()).//
                    build();
        } catch (Exception e) {
            log.error("鉴权失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "鉴权失败");
        }
        return httpUrl.toString();
    }


    /**
     * 发起http的post请求
     * @param url
     * @param urlParams
     * @param json
     * @return
     */
    public String doPostJson(String url, Map<String, String> urlParams, String json) {
        if (StrUtil.isBlank(url)) {
            log.error("url为空");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "url为空");
        }
        //创建HttpClient对象
        try (CloseableHttpClient closeableHttpClient = HttpClients.createDefault()) {
            String asciiUrl = URI.create(url).toASCIIString();
            RequestBuilder builder = RequestBuilder.post(asciiUrl).setCharset(StandardCharsets.UTF_8);
            //拼接参数
            if (urlParams != null) {
                urlParams.forEach(builder::addParameter);
            }
            //设置请求体
            StringEntity requestEntry = new StringEntity(json, ContentType.APPLICATION_JSON);
            builder.setEntity(requestEntry);
            //执行请求
            try(CloseableHttpResponse response = closeableHttpClient.execute(builder.build())){
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }catch (Exception e){
                log.error("执行请求失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "执行请求失败");
            }
        } catch (Exception e) {
            log.error("发起post请求失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发起post请求失败");
        }
    }
}

class JsonParse {
    Payload payload;
}

class Payload {
    Choices choices;
}

class Choices {
    List<Text> text;
}

class Text {
    String content;
}


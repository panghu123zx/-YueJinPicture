package com.ph.phpictureback.api.imageSearch.submit;

import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class getImageFirstUrlApi {

    /**
     * 获取图片列表界面地址
     *
     * @param url
     * @return
     */
    public static String getImageFirstUrl(String url) {
        try {
            Document document = Jsoup.connect(url).timeout(5000).get();
            //获取script标签
            Elements script = document.getElementsByTag("script");
            //遍历 firstUrl中的类容
            for (Element ele : script) {
                String scriptContent = ele.html();
                if (scriptContent.contains("\"firstUrl\"")) {
                    //正则表达式提取firstUrl的值
                    Pattern compile = Pattern.compile("\"firstUrl\"\\s*:\\s*\"(.*?)\"");
                    Matcher matcher = compile.matcher(scriptContent);
                    if (matcher.find()) {
                        String firstUrl = matcher.group(1);
                        //处理转移字符
                        firstUrl = firstUrl.replace("\\/", "/");
                        return firstUrl;
                    }
                }
            }

            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到url");
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }


    public static void main(String[] args) {
        String url = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData[isLogoShow]=1&f=all&isLogoShow=1&session_id=17807770648778027261&sign=1215fe97cd54acd88139901742483715&tpl_from=pc";
        String imageFirstUrl = getImageFirstUrl(url);
        System.out.println(imageFirstUrl);
    }
}

package com.ph.phpictureback.api.imageSearch.submit;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.api.imageSearch.model.ImageSearchDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class getImageListApi {


    /**
     * 获取图片列表
     * @param url
     * @return
     */
    public static List<ImageSearchDto> getImageList(String url){
        try {
            //发送get请求
            HttpResponse response = HttpUtil.createGet(url).execute();

            //获取响应结果
            String result = response.body();
            int status = response.getStatus();
            //处理响应
            if (status == 200) {
                //解析json 数据，并处理数据
                return processResponse(result);
            }else{
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"接口调用失败");
            }
        } catch (Exception e) {
            log.error("获取图片列表失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取图片列表失败");
        }
    }

    /**
     * 处理接口响应内容
     * @param result
     * @return
     */
    private static List<ImageSearchDto> processResponse(String result) {
        //解析响应对象
        JSONObject jsonObject = new JSONObject(result);
        if (!jsonObject.containsKey("data")){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"未获取到图片列表");
        }

        JSONObject data = jsonObject.getJSONObject("data");
        if (!data.containsKey("list")){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"未获取到图片列表");
        }
        JSONArray list = data.getJSONArray("list");
        //封装返回结果
        return JSONUtil.toList(list,ImageSearchDto.class);
    }

    public static void main(String[] args) {
        String url="https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&" +
                "inspire=general_pc&limit=30&next=2&render_type=card&session_id=17807770648778027261&s" +
                "ign=1215fe97cd54acd88139901742483715&tk=1ba68&tpl_from=pc";
        List<ImageSearchDto> imageList = getImageList(url);
        System.out.println(imageList);
    }


}

package com.ph.phpictureback.api.imageSearch;

import com.ph.phpictureback.api.imageSearch.submit.getImageUrlApi;
import com.ph.phpictureback.api.imageSearch.model.ImageSearchDto;
import com.ph.phpictureback.api.imageSearch.submit.getImageFirstUrlApi;
import com.ph.phpictureback.api.imageSearch.submit.getImageListApi;

import java.util.List;

/**
 * 门面模式，简化调用
 */
public class imageSearchFacade {

    /**
     * 以图搜图结果图片列表返回
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchDto> getImageSearchList(String imageUrl){
        String url = getImageUrlApi.getImageUrl(imageUrl);
        String jsonUrl = getImageFirstUrlApi.getImageFirstUrl(url);
        List<ImageSearchDto> imageList = getImageListApi.getImageList(jsonUrl);
        return imageList;
    }

    public static void main(String[] args) {
        String url = "https://www.codefather.cn/logo.png";
        List<ImageSearchDto> imageSearchList = getImageSearchList(url);
        System.out.println(imageSearchList);
    }
}

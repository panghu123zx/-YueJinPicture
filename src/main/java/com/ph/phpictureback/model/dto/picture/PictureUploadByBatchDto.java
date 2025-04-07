package com.ph.phpictureback.model.dto.picture;

import lombok.Data;

@Data
public class PictureUploadByBatchDto {
  
    /**  
     * 搜索词  
     */  
    private String searchText;  
  
    /**  
     * 抓取数量  
     */  
    private Integer count = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;

}

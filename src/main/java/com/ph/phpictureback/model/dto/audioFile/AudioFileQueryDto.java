package com.ph.phpictureback.model.dto.audioFile;

import com.ph.phpictureback.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AudioFileQueryDto extends PageRequest {


    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 文件类型 0-图片，1-视频，2-音频
     */
    private Integer fileType;

    /**
     * 标题
     */
    private String title;

    /**
     * 大小
     */
    private Long size;

    /**
     * 简介
     */
    private String introduction;

}

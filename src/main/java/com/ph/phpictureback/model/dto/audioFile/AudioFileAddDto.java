package com.ph.phpictureback.model.dto.audioFile;

import lombok.Data;

@Data
public class AudioFileAddDto {

    /**
     * 文件类型 0-图片，1-视频，2-音频
     */
    private Integer fileType;

    /**
     * 标题
     */
    private String title;

    /**
     * 简介
     */
    private String introduction;



}

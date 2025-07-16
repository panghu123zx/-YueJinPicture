package com.ph.phpictureback.model.dto.audioFile;

import lombok.Data;

@Data
public class AudioFileUpdateDto {


    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 简介
     */
    private String introduction;
}

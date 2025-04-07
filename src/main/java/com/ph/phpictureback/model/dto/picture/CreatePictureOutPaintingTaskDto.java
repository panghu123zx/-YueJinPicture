package com.ph.phpictureback.model.dto.picture;

import com.ph.phpictureback.api.aliyun.model.CreateOutPaintingTaskDto;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskDto implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskDto.Parameters parameters;

    private static final long serialVersionUID = 1L;
}

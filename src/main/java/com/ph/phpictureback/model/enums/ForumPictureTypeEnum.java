package com.ph.phpictureback.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 点赞或分享
 */
@Getter
public enum ForumPictureTypeEnum {
    PICTURE("图片", 0),
    FORUM("帖子", 1);


    private final String text;
    private final Integer value;

    ForumPictureTypeEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value进行枚举
     *
     * @param value
     * @return
     */
    public static ForumPictureTypeEnum getForumPictureTypeValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ForumPictureTypeEnum anEnum : ForumPictureTypeEnum.values()) {
            if (anEnum.getValue().equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

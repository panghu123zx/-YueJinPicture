package com.ph.phpictureback.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 点赞或分享
 */
@Getter
public enum UserLikeTypeEnum {
    LIKE("点赞", 0),
    SHARE("分享", 1);


    private final String text;
    private final Integer value;

    UserLikeTypeEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value进行枚举
     *
     * @param value
     * @return
     */
    public static UserLikeTypeEnum getUserLikeTypeValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserLikeTypeEnum anEnum : UserLikeTypeEnum.values()) {
            if (anEnum.getValue().equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

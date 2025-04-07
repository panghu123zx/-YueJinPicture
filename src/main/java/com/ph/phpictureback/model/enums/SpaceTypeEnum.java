package com.ph.phpictureback.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 空间类型的枚举
 */
@Getter
public enum SpaceTypeEnum {
    PRIVATE("私人空间", 0),
    TEAM("团队空间", 1);


    private final String text;
    private final Integer value;

    SpaceTypeEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value进行枚举
     *
     * @param value
     * @return
     */
    public static SpaceTypeEnum getSpaceTypeValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceTypeEnum anEnum : SpaceTypeEnum.values()) {
            if (anEnum.getValue().equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

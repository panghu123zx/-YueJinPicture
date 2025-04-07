package com.ph.phpictureback.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 空间级别的枚举
 */
@Getter
public enum SpaceLevelEnum {
    COMMON("普通版", 0, 100, 100L * 1024 * 1024),
    PROFESSIONAL("专业版", 1, 1000, 1000L * 1024 * 1024),
    FLAGSHIP("旗舰版", 2, 10000, 10000L * 1024 * 1024);


    private final String text;
    private final Integer value;
    private final long maxCount;
    private final long maxSize;

    SpaceLevelEnum(String text, Integer value, long maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    /**
     * 根据value进行枚举
     *
     * @param value
     * @return
     */
    public static SpaceLevelEnum getSpaceLevelValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceLevelEnum anEnum : SpaceLevelEnum.values()) {
            if (anEnum.getValue().equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

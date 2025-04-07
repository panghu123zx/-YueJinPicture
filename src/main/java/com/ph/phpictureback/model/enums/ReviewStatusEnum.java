package com.ph.phpictureback.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户权限的枚举
 */
@Getter
public enum ReviewStatusEnum {
    //状态：0-待审核; 1-通过; 2-拒绝
    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);


    private final String text;
    private final Integer value;

    ReviewStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value进行枚举
     *
     * @param value
     * @return
     */
    public static ReviewStatusEnum getReviewStatusValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ReviewStatusEnum anEnum : ReviewStatusEnum.values()) {
            if (anEnum.getValue().equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

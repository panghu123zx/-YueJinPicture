package com.ph.phpictureback.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * 用户权限的枚举
 */
@Getter
public enum SpaceUserEnum {
    VIEWER("浏览者", "viewer"),
    EDITOR("编辑者", "editor"),
    ADMIN("管理员", "admin");


    private final String text;
    private final String value;

    SpaceUserEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value进行枚举
     *
     * @param value
     * @return
     */
    public static SpaceUserEnum getUserRoleValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceUserEnum anEnum : SpaceUserEnum.values()) {
            if (anEnum.getValue().equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 获取枚举的text
     *
     * @param spaceUser
     * @return
     */
    public static List<String> getSpaceUserText(String spaceUser) {
        return Arrays.stream(SpaceUserEnum.values())
                .map(SpaceUserEnum::getText).
                collect(Collectors.toList());
    }

    /**
     * 获取枚举的value
     * @params spaceUser
     * @return
     */
    public static List<String> getSpaceUserValue(String spaceUser) {
        return Arrays.stream(SpaceUserEnum.values())
                .map(SpaceUserEnum::getValue).
                collect(Collectors.toList());
    }
}

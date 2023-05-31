package com.hao14293.im.common.enums;

/**
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public enum ImUserTypeEnum {
    IM_USER(1),
    APP_ADMIN(100),
        ;

    private int code;

    ImUserTypeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

package com.hao14293.im.common.enums;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public enum UserSilentFlagEnum {
    /**
     * 0 正常；1 禁言。
     */
    NORMAL(0),

    MUTE(1),
    ;

    private int code;

    public int getCode() {
        return code;
    }

    UserSilentFlagEnum(int code) {
        this.code = code;
    }
}

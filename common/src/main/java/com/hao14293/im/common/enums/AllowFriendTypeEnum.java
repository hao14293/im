package com.hao14293.im.common.enums;

/**
 * 加好友是否需要验证
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public enum AllowFriendTypeEnum {
    // 是否需要验证
    NOT_NEED(1),
    NEED(2),
    ;
    private int code;

    AllowFriendTypeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

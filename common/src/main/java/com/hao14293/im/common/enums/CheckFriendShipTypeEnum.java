package com.hao14293.im.common.enums;

/**
 * 好友关系类型
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public enum CheckFriendShipTypeEnum {
    SINGLE(1),
    BOTH(2),
    ;

    private int code;

    CheckFriendShipTypeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

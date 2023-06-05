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

    private int type;

    CheckFriendShipTypeEnum(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}

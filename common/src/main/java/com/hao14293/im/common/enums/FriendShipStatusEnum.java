package com.hao14293.im.common.enums;

/**
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public enum FriendShipStatusEnum {
    /**
     * 0 未添加 1 正常 2 删除
     */
    FRIEND_STATUS_NOT_FRIEND(0),
    FRIEND_STATUS_NORMAL(1),
    FRIEND_STATUS_DELETE(2),

    BLACK_STATUS_NORMAL(1),
    BLACK_STATUS_BLACKED(2),
    ;

    private int code;

    FriendShipStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

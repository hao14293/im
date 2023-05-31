package com.hao14293.im.common.enums;

/**
 * 处理好友申请
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public enum ApproverFriendRequestStatusEnum {
    /**
     * 1 同意 2 拒绝
     */
    AGREE(1),
    REJECT(2),
    ;
    private int code;

    ApproverFriendRequestStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

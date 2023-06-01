package com.hao14293.im.common.enums;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public enum GroupMuteTypeEnum {
    /**
     * 是否全员禁言，0 不禁言；1 全员禁言。
     */
    NOT_MUTE(0),
    MUTE(1),
    ;

    private int code;

    GroupMuteTypeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

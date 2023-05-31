package com.hao14293.im.common.enums;

/**
 * 好友是否删除
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public enum DelFlagEnum {
    /**
     * 0 正常 1 删除
     */
    NORMAL(0),
    DELETE(1),
    ;

    private int code;

    DelFlagEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

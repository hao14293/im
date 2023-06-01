package com.hao14293.im.common.enums;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public enum GroupStatusEnum {
    /**
     * 1正常 2解散 其他待定比如封禁...
     */
    NORMAL(1),

    DESTROY(2),
    ;

    private int code;

    GroupStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

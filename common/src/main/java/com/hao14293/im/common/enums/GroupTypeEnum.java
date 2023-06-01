package com.hao14293.im.common.enums;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public enum GroupTypeEnum {
    /**
     * 群类型 1私有群（类似微信） 2公开群(类似qq）
     */
    PRIVATE(1),

    PUBLIC(2),
    ;

    private int code;

    GroupTypeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

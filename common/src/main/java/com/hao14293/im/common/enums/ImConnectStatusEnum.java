package com.hao14293.im.common.enums;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public enum ImConnectStatusEnum {
    /**
     * 管道链接状态,1=在线，2=离线。。
     */
    ONLINE_STATUS(1),

    OFFLINE_STATUS(2),
    ;

    private int code;

    ImConnectStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

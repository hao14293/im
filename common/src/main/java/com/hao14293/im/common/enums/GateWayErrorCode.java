package com.hao14293.im.common.enums;

import com.hao14293.im.common.exception.ApplicationExceptionEnum;

/**
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public enum GateWayErrorCode implements ApplicationExceptionEnum {
    USERSIGN_NOT_EXIST(60000, "用户签名不存在"),
    APPID_NOT_EXIST(60001, "appid不存在"),
    OPERATER_NOT_EXIST(60002, "操作不存在"),
    USERSIGN_IS_ERROR(60003, "用户签名不存在"),
    USERSIGN_IS_EXPIRED(60004, "用户签名已过期"),
    USERSIGN_OPERATE_NOT_MATE(60005, "用户签名与操作人不匹配"),
    ;

    private int code;
    private String error;

    GateWayErrorCode(int code, String error) {
        this.code = code;
        this.error = error;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getError() {
        return this.error;
    }
}

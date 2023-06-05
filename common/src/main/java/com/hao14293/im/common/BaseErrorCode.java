package com.hao14293.im.common;

import com.hao14293.im.common.exception.ApplicationExceptionEnum;

/**
 * 通用错误码
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public enum BaseErrorCode implements ApplicationExceptionEnum {
    SUCCESS(200, "success"),
    SYSTEM_ERROR(90000, "服务器内部错误，请联系管理员"),
    PARAMETER_ERROR(90001, "参数检验错误")
    ;

    private int code;
    private String error;

    BaseErrorCode(int code, String error) {
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

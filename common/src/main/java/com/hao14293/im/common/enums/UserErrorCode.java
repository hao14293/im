package com.hao14293.im.common.enums;

import com.hao14293.im.common.exception.ApplicationExceptionEnum;

/**
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public enum UserErrorCode implements ApplicationExceptionEnum {

    IMPORT_SIZE_BEYOND(20000, "导入数量超出上限"),
    USER_IS_NOT_EXIST(20001, "用户不存在"),
    SERVER_GET_USER_ERROR(20002, "服务获取用户失败"),
    MODIFY_USER_ERROR(20003,"更新用户失败"),
    SERVER_NOT_AVALIABLE(71000, "没有可用的服务"),
    ;

    private int code;
    private String error;

    UserErrorCode(int code, String error) {
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

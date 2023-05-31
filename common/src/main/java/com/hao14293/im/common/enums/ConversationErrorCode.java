package com.hao14293.im.common.enums;

import com.hao14293.im.common.exception.ApplicationExceptionEnum;

/**
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public enum ConversationErrorCode implements ApplicationExceptionEnum {
    CONVERSATION_UPDATE_PARAM_ERROR(50000, "回话参数修改错误"),
    ;

    private int code;
    private String error;

    ConversationErrorCode(int code, String error) {
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

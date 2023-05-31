package com.hao14293.im.common.exception;

/**
 * 全局异常处理类
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public class ApplicationException extends RuntimeException{
    // 状态码
    private int code;
    // error信息
    private String error;

    public ApplicationException(ApplicationExceptionEnum exceptionEnum) {
        super(exceptionEnum.getError());
        this.code = exceptionEnum.getCode();
        this.error = exceptionEnum.getError();
    }

    public ApplicationException(int code, String error) {
        super(error);
        this.code = code;
        this.error = error;
    }

    public int getCode() {
        return code;
    }

    public String getError() {
        return error;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}

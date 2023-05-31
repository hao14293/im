package com.hao14293.im.common;

import com.hao14293.im.common.exception.ApplicationExceptionEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用返回类
 * @Author: hao14293
 * @Date: 2023/5/31
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings("all")
public class ResponseVO<T> {
    // 状态码
    private int code;
    private String msg;
    private T data;

    public static ResponseVO successResponse(Object data) {
        return new ResponseVO(200, "success", data);
    }

    public static ResponseVO successResponse() {
        return new ResponseVO(200, "success");
    }

    public static ResponseVO errorResponse() {
        return new ResponseVO(500, "系统内部错误");
    }

    public static ResponseVO errorResponse(int code, String msg) {
        return new ResponseVO(code, msg);
    }

    public static ResponseVO errorResponse(ApplicationExceptionEnum enums) {
        return new ResponseVO(enums.getCode(), enums.getError());
    }

    public boolean isOk() {
        return this.code == 200;
    }

    public ResponseVO(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ResponseVO success() {
        this.code = 200;
        this.msg = "success";
        return this;
    }

    public ResponseVO success(T data) {
        this.code = 200;
        this.msg = "success";
        this.data = data;
        return this;
    }
}

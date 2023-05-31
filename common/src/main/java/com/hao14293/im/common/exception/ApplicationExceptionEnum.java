package com.hao14293.im.common.exception;

/**
 * 全局异常处理枚举接口,
 * 用接口做参数，就可以直接调用 接口的方法
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public interface ApplicationExceptionEnum {

    // 状态码
    int getCode();
    // error信息
    String getError();
}

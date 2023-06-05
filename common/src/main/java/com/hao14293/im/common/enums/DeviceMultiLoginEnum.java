package com.hao14293.im.common.enums;

/**
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public enum DeviceMultiLoginEnum {
    /**
     * 单端登录 仅允许 Windows Web Andriod IOS单端登录
     */
    ONE(1, "DeviceMultiLoginEnum_ONE"),
    /**
     * 允许双端登录
     */
    TWO(2, "DeviceMultiLoginEnum_TWO"),
    /**
     * 三端登录
     */
    THREE(3, "DeviceMultiLoginEnum_THREE"),
    /**
     * 多端同时
     */
    ALL(4, "DeviceMultiLoginEnum_ALL"),
    ;

    private int loginMode;
    private String loginDesc;

    DeviceMultiLoginEnum(int loginMode, String loginDesc) {
        this.loginMode = loginMode;
        this.loginDesc = loginDesc;
    }

    public int getLoginMode() {
        return loginMode;
    }

    public String getLoginDesc() {
        return loginDesc;
    }
}

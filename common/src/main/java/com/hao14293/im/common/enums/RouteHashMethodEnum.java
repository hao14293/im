package com.hao14293.im.common.enums;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public enum RouteHashMethodEnum {
    /**
     * TreeMap
     */
    TREE(1,"com.hao14293.im.common.route.algorithm.consistenthash" +
            ".TreeMapConsistentHash"),
    /**
     * 自定义map
     */
    CUSTOMER(2,"com.hao14293.im.common.route.algorithm.consistenthash.xxxx"),
    ;

    private int code;
    private String clazz;

    public static RouteHashMethodEnum getHandler(int ordinal) {
        for (int i = 0; i < RouteHashMethodEnum.values().length; i++) {
            if (RouteHashMethodEnum.values()[i].getCode() == ordinal) {
                return RouteHashMethodEnum.values()[i];
            }
        }
        return null;
    }

    RouteHashMethodEnum(int code, String clazz) {
        this.code = code;
        this.clazz = clazz;
    }

    public int getCode() {
        return code;
    }

    public String getClazz() {
        return clazz;
    }
}

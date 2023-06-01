package com.hao14293.im.common.enums;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
/**
 * 负载均衡策略，通过配置文件加载
 */
public enum ImUrlRouteWayEnum {
    /**
     * 随机
     */
    RAMDOM(1,"com.hao14293.im.common.route.algorithm.random.RandomHandle"),
    /**
     * 1.轮训
     */
    LOOP(2,"com.hao14293.im.common.route.algorithm.loop.LoopHandle"),
    /**
     * HASH
     */
    HASH(3,"com.hao14293.im.common.route.algorithm.consistenthash.ConsistentHashHandle"),
    ;
    private int code;
    private String clazz;

    ImUrlRouteWayEnum(int code, String clazz) {
        this.code = code;
        this.clazz = clazz;
    }

    /**
     * 不能用 默认的 enumType b= enumType.values()[i]; 因为本枚举是类形式封装
     * @param ordinal
     * @return
     */
    public static ImUrlRouteWayEnum getHandler(int ordinal) {
        for (int i = 0; i < ImUrlRouteWayEnum.values().length; i++) {
            if (ImUrlRouteWayEnum.values()[i].getCode() == ordinal) {
                return ImUrlRouteWayEnum.values()[i];
            }
        }
        return null;
    }

    public int getCode() {
        return code;
    }

    public String getClazz() {
        return clazz;
    }
}

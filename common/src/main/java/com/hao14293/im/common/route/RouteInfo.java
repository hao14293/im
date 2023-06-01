package com.hao14293.im.common.route;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public final class RouteInfo {

    private String ip;

    private Integer port;

    public RouteInfo(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }
}

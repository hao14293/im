package com.hao14293.im.common.model;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class UserSession {

    // 用户Id
    private String userId;

    // 应用id
    private Integer appId;

    // 端的标识
    private Integer clientType;

    // sdk 版本号
    private Integer version;

    // 连接状态 1在线 2离线
    private Integer connectState;

    // brokerId
    private Integer brokerId;

    // brookerHost
    private String brokerHost;

    private String imei;
}

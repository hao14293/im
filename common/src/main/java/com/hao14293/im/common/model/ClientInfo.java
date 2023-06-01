package com.hao14293.im.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
@NoArgsConstructor
public class ClientInfo {

    private Integer appId;

    private Integer clientType;

    private String imei;

    public ClientInfo(Integer appId, Integer clientType, String imei) {
        this.appId = appId;
        this.clientType = clientType;
        this.imei = imei;
    }
}

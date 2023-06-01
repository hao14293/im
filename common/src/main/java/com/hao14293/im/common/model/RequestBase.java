package com.hao14293.im.common.model;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class RequestBase {

    private Integer appId;

    private String operater;

    private Integer clientType;

    private String imei;
}

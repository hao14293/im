package com.hao14293.im.common.model;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class UserClientDto {

    private Integer appId;

    private Integer clientType;

    private String userId;

    private String imei;
}

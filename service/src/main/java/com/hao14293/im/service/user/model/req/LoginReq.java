package com.hao14293.im.service.user.model.req;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class LoginReq {

    @NotNull(message = "用户id不能为空")
    private String userId;

    @NotNull(message = "appId 不能为空")
    private Integer appId;

    private Integer clientType;
}

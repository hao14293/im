package com.hao14293.im.service.user.model.req;

import com.hao14293.im.common.model.RequestBase;
import lombok.Data;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class SubscribeUserOnlineStatusReq extends RequestBase {

    private List<String> subUserId;

    private Long subTime;
}

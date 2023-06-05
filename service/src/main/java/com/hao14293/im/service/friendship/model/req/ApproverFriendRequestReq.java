package com.hao14293.im.service.friendship.model.req;

import com.hao14293.im.common.model.RequestBase;
import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
public class ApproverFriendRequestReq extends RequestBase {

    private Long id;

    //1同意 2拒绝
    private Integer status;
}

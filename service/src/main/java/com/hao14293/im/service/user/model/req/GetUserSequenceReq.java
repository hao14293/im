package com.hao14293.im.service.user.model.req;

import com.hao14293.im.common.model.RequestBase;
import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class GetUserSequenceReq extends RequestBase {

    private String userId;
}

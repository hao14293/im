package com.hao14293.im.common.model.message;

import com.hao14293.im.common.model.ClientInfo;
import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class RecallMessageContent extends ClientInfo {
    /**
        {
            "messageKey": 419455774914383872,
            "fromId":"lld",
            "toId":"lld4",
            "messageTime":"1665026849851",
            "messageSequence":2,
            "appId": 10000,
            "clientType": 1,
            "imei": "web",
            "conversationType":0
        }
     */

    private Long messageKey;

    private String fromId;

    private String toId;

    private Long messageTime;

    private Long messageSequence;

    private Integer conversationType;
}

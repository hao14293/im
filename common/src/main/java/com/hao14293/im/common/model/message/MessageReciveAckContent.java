package com.hao14293.im.common.model.message;

import com.hao14293.im.common.model.ClientInfo;
import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class MessageReciveAckContent extends ClientInfo {

    private Long messageKey;

    private String fromId;

    private String toId;

    private Long messageSequence;
}

package com.hao14293.im.common.model.message;

import com.hao14293.im.common.model.ClientInfo;
import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class MessageContent extends ClientInfo {

    private String messageId;

    private String fromId;

    private String toId;

    private String messageBody;

    private Long messageTime;

    private String extra;

    private Long messageKey;

    private long messageSequence;
}

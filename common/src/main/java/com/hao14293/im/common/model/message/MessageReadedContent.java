package com.hao14293.im.common.model.message;

import com.hao14293.im.common.model.ClientInfo;
import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class MessageReadedContent extends ClientInfo {

    private long messageSequence;

    private String fromId;

    private String groupId;

    private String toId;

    // 会话类型
    private Integer conversationType;
}

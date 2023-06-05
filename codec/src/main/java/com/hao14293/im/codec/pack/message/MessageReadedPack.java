package com.hao14293.im.codec.pack.message;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
public class MessageReadedPack {

    private long messageSequence;

    private String fromId;

    private String groupId;

    private String toId;

    private Integer conversationType;
}


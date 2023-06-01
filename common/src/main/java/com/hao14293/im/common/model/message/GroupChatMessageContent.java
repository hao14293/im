package com.hao14293.im.common.model.message;

import lombok.Data;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class GroupChatMessageContent extends MessageContent{

    private String groupId;

    private List<String> memberId;
}

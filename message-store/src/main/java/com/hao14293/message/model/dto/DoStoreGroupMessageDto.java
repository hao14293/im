package com.hao14293.message.model.dto;

import com.hao14293.im.common.model.message.GroupChatMessageContent;
import com.hao14293.message.model.entity.ImMessageBodyEntity;
import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
public class DoStoreGroupMessageDto {

    private GroupChatMessageContent groupChatMessageContent;

    private ImMessageBodyEntity imMessageBodyEntity;

}
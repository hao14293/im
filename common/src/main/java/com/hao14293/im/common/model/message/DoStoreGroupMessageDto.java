package com.hao14293.im.common.model.message;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class DoStoreGroupMessageDto {

    private GroupChatMessageContent groupChatMessageContent;

    private ImMessageBody messageBody;
}

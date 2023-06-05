package com.hao14293.im.service.conversation.model.req;

import com.hao14293.im.common.model.RequestBase;
import lombok.Data;


/**
 * @author hao14293
 */
@Data
public class UpdateConversationReq extends RequestBase {

    private String conversationId;

    // 是否禁言
    private Integer isMute;

    // 是否置顶
    private Integer isTop;

    private String fromId;
}

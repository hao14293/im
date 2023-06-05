package com.hao14293.im.service.conversation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


/**
 * @author hao14293
 */
@Data
@TableName("im_conversation_set")
public class ImConversationSetEntity {

    //会话id 0_fromId_toId
    private String conversationId;

    //会话类型
    private Integer conversationType;

    private String fromId;

    private String toId;

    private int isMute;

    private int isTop;

    private Long sequence;

    private Long readedSequence;

    private Integer appId;
}

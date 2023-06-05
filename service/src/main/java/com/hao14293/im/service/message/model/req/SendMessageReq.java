package com.hao14293.im.service.message.model.req;

import com.hao14293.im.common.model.RequestBase;
import lombok.Data;

/**
 * @Author: hao14293
 * @data 2023/4/26
 * @time 10:49
 */
@Data
public class SendMessageReq extends RequestBase {

    //客户端传的messageId
    private String messageId;

    private String fromId;

    private String toId;

    private int messageRandom;

    private long messageTime;

    private String messageBody;
    /**
     * 这个字段缺省或者为 0 表示需要计数，为 1 表示本条消息不需要计数，即右上角图标数字不增加
     */
    private int badgeMode;

    private Long messageLifeTime;

    private Integer appId;

}



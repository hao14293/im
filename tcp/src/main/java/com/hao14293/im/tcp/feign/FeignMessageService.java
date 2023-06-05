package com.hao14293.im.tcp.feign;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.model.message.CheckSendMessageReq;
import feign.Headers;
import feign.RequestLine;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
public interface FeignMessageService {

    @Headers({"Content-Type: application/json", "Accept: application/json"})
    @RequestLine("POST /message/checkSend")
    public ResponseVO checkSendMessage(CheckSendMessageReq checkSendMessageReq);
}

package com.hao14293.im.service.message.controller;


import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.model.SyncReq;
import com.hao14293.im.common.model.message.CheckSendMessageReq;
import com.hao14293.im.service.group.service.GroupMessageService;
import com.hao14293.im.service.message.model.req.SendMessageReq;
import com.hao14293.im.service.message.service.MessageSyncService;
import com.hao14293.im.service.message.service.P2PMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: hao14293
 * @data 2023/4/26
 * @time 10:45
 */
@RestController
@RequestMapping("/v1/message")
public class MessageController {

    @Autowired
    private P2PMessageService p2PMessageService;

     @Autowired
    private GroupMessageService groupMessageService;

    @Autowired
    private MessageSyncService messageSyncService;

    @RequestMapping("/send")
    public ResponseVO send(@RequestBody SendMessageReq req, Integer appId){
        req.setAppId(appId);
        return ResponseVO.successResponse(p2PMessageService.send(req));
    }

    @RequestMapping("/checkSend")
    public ResponseVO checkSend(@RequestBody CheckSendMessageReq req){
        return p2PMessageService.imeServerPermissionCheck(req.getFromId(), req.getToId(), req.getAppId());
    }

    @RequestMapping("/syncOfflineMessage")
    public ResponseVO syncOfflineMessage(@RequestBody SyncReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return messageSyncService.syncOfflineMessage(req);
    }
}

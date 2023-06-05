package com.hao14293.im.service.conversation.controller;


import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.model.SyncReq;
import com.hao14293.im.service.conversation.model.req.DeleteConversationReq;
import com.hao14293.im.service.conversation.model.req.UpdateConversationReq;
import com.hao14293.im.service.conversation.service.ConversationService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


/**
 * @author hao14293
 */
@RestController
@RequestMapping("/v1/conversation")
public class ConverstaionController {

    @Resource
    private ConversationService conversationService;

    @RequestMapping("/deleteConversation")
    public ResponseVO deleteConversation(@RequestBody DeleteConversationReq
                                                 req, Integer appId, String identifier)  {
        req.setAppId(appId);
//        req.setOperater(identifier);
        return conversationService.deleteConversation(req);
    }

    @RequestMapping("/updateConversation")
    public ResponseVO updateConversation(@RequestBody UpdateConversationReq
                                                 req, Integer appId, String identifier)  {
        req.setAppId(appId);
//        req.setOperater(identifier);
        return conversationService.updateConversation(req);
    }

    @RequestMapping("/syncConversationList")
    public ResponseVO syncFriendShipList(@RequestBody SyncReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return conversationService.syncConversationSet(req);
    }

}

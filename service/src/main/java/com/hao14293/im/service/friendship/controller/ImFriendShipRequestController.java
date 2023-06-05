package com.hao14293.im.service.friendship.controller;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.friendship.model.req.ApproverFriendRequestReq;
import com.hao14293.im.service.friendship.model.req.FriendDto;
import com.hao14293.im.service.friendship.model.req.GetFriendShipRequestReq;
import com.hao14293.im.service.friendship.model.req.ReadFriendShipRequestReq;
import com.hao14293.im.service.friendship.service.ImFriendShipRequestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */

@Api(tags = "FriendRequest")
@RestController
@RequestMapping("v1/friendshipRequest")
public class ImFriendShipRequestController {

    @Resource
    ImFriendShipRequestService imFriendShipRequestService;

    @ApiOperation("新增好友请求")
    @RequestMapping("/addFriendRequest")
    public ResponseVO addFriendRequest(@RequestBody FriendDto dto, String fromId, Integer appId){
        return imFriendShipRequestService.addFriendshipRequest(fromId, dto, appId);
    }

    @ApiOperation("审批好友请求")
    @RequestMapping("/approveFriendRequest")
    public ResponseVO approveFriendRequest(@RequestBody ApproverFriendRequestReq req, Integer appId, String identifier){
        req.setAppId(appId);
        req.setOperater(identifier);
        return imFriendShipRequestService.approverFriendRequest(req);
    }

    @ApiOperation("已读请求")
    @RequestMapping("/readFriendShipRequestReq")
    public ResponseVO readFriendShipRequestReq(@RequestBody ReadFriendShipRequestReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipRequestService.readFriendShipRequestReq(req);
    }

    @RequestMapping("/getFriendRequest")
    public ResponseVO getFriendRequest(@RequestBody @Validated GetFriendShipRequestReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipRequestService.getFriendRequest(req.getFromId(),req.getAppId());
    }
}



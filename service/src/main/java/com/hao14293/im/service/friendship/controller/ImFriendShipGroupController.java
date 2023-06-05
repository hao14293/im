package com.hao14293.im.service.friendship.controller;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.hao14293.im.service.friendship.model.req.AddFriendShipGroupReq;
import com.hao14293.im.service.friendship.model.req.DeleteFriendShipGroupMemberReq;
import com.hao14293.im.service.friendship.model.req.DeleteFriendShipGroupReq;
import com.hao14293.im.service.friendship.service.ImFriendShipGroupMemberService;
import com.hao14293.im.service.friendship.service.ImFriendShipGroupService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Api(tags = "FriendGroup")
@RestController
@RequestMapping("v1/friendship/group")
public class ImFriendShipGroupController {
    @Resource
    ImFriendShipGroupService imFriendShipGroupService;

    @Resource
    ImFriendShipGroupMemberService imFriendShipGroupMemberService;

    @ApiOperation("新增好友分组")
    @RequestMapping("/add")
    public ResponseVO add(@RequestBody AddFriendShipGroupReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendShipGroupService.addGroup(req);
    }

    @ApiOperation("删除好友分组")
    @RequestMapping("/del")
    public ResponseVO del(@RequestBody DeleteFriendShipGroupReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendShipGroupService.deleteGroup(req);
    }

    @ApiOperation("新增好友分组成员")
    @RequestMapping("/member/add")
    public ResponseVO memberAdd(@RequestBody AddFriendShipGroupMemberReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendShipGroupMemberService.addGroupMember(req);
    }

    @ApiOperation("删除好友分组成员")
    @RequestMapping("/member/del")
    public ResponseVO memberdel(@RequestBody DeleteFriendShipGroupMemberReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendShipGroupMemberService.delGroupMember(req);
    }
}

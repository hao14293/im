package com.hao14293.im.service.group.controller;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.model.SyncReq;
import com.hao14293.im.service.group.model.req.*;
import com.hao14293.im.service.group.service.GroupMessageService;
import com.hao14293.im.service.group.service.ImGroupService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Api(tags = "Group")
@RestController
@RequestMapping("/v1/group")
public class ImGroupController {

    @Resource
    private ImGroupService groupService;

    @Resource
    private GroupMessageService groupMessageService;

    @ApiOperation(value = "导入群组")
    @PostMapping("/importGroup")
    public ResponseVO importGroup(@RequestBody ImportGroupReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperater(identifier);
        return groupService.importGroup(req);
    }

    @ApiOperation("新建群组")
    @PostMapping("/createGroup")
    public ResponseVO createGroup(@RequestBody CreateGroupReq req, Integer appId, String identifier){
        req.setAppId(appId);
        req.setOperater(identifier);
        return groupService.createGroup(req);
    }

    @ApiOperation("修改群消息")
    @PostMapping("/updateGroupInfo")
    public ResponseVO updateGroupInfo(@RequestBody UpdateGroupReq req, Integer appId, String identifier){
        req.setAppId(appId);
        req.setOperater(identifier);
        return groupService.updateBaseGroupInfo(req);
    }

    @ApiOperation("获取群组的具体信息")
    @PostMapping("/getGroupInfo")
    public ResponseVO getGroupInfo(@RequestBody GetGroupInfoReq req, Integer appId, String identifier){
        req.setAppId(appId);
        req.setOperater(identifier);
        return groupService.getGroupInfo(req);
    }

    @ApiOperation("获取用户加入的群组列表")
    @PostMapping("/getJoinedGroup")
    public ResponseVO getJoinedGroup(@RequestBody GetJoinedGroupReq req, Integer appId, String identifier){
        req.setAppId(appId);
        req.setOperater(identifier);
        return groupService.getJoinedGroup(req);
    }

    @ApiOperation("解散群组")
    @PostMapping("/destroyGroup")
    public ResponseVO destroyGroup(@RequestBody DestroyGroupReq req, Integer appId, String identifier){
        req.setAppId(appId);
        req.setOperater(identifier);
        return groupService.destroyGroup(req);
    }

    @ApiOperation("转让群主")
    @PostMapping("/transferGroup")
    public ResponseVO transferGroup(@RequestBody TransferGroupReq req, Integer appId, String identifier){
        req.setAppId(appId);
        req.setOperater(identifier);
        return groupService.transferGroup(req);
    }

    @ApiOperation("群组禁言")
    @PostMapping("/forbidSendMessage")
    public ResponseVO forbidSendMessage(@RequestBody MuteGroupReq req, Integer appId, String identifier){
        req.setAppId(appId);
        req.setOperater(identifier);
        return groupService.muteGroup(req);
    }

    @ApiOperation("发送群聊消息")
    @RequestMapping("/sendMessage")
    public ResponseVO sendMessage(@RequestBody SendGroupMessageReq req, Integer appId, String identifier){
        req.setAppId(appId);
        req.setOperater(identifier);
        return ResponseVO.successResponse(groupMessageService.send(req));
    }

    @ApiOperation("同步加入的群组列表（增量拉取）")
    @RequestMapping("/syncJoinedGroup")
    public ResponseVO syncJoinedGroup(@RequestBody SyncReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return groupService.syncJoinedGroupList(req);
    }
}

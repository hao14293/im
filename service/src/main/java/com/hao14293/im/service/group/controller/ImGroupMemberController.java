package com.hao14293.im.service.group.controller;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.group.model.req.*;
import com.hao14293.im.service.group.service.ImGroupMemberService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author: hao14293
 * @Date: 2023/6/2
 */
@Api(tags = "GroupMember")
@RestController
@RequestMapping("/v1/group/member")
public class ImGroupMemberController {

    @Resource
    private ImGroupMemberService imGroupMemberService;


    @ApiOperation("导入群组成员")
    @PostMapping("/importGroupMember")
    public ResponseVO importGroupMember(@RequestBody ImportGroupMemberReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return imGroupMemberService.importGroupMember(req);
    }

    @ApiOperation("拉人入群")
    @PostMapping("/addMember")
    public ResponseVO addMember(@RequestBody AddGroupMemberReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return imGroupMemberService.addMember(req);
    }

    @ApiOperation("踢人")
    @PostMapping("/removeMember")
    public ResponseVO removeMember(@RequestBody RemoveGroupMemberReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return imGroupMemberService.removeMember(req);
    }

    @ApiOperation("退出群聊")
    @PostMapping("/exitGroup")
    public ResponseVO exitGroup(@RequestBody ExitGroupReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return imGroupMemberService.exitGroup(req);
    }

    @ApiOperation("修改群成员信息")
    @PostMapping("/updateGroupMember")
    public ResponseVO updateGroupMember(@RequestBody UpdateGroupMemberReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return imGroupMemberService.updateGroupMember(req);
    }

    @ApiOperation("禁言群组成员")
    @PostMapping("/speak")
    public ResponseVO speak(@RequestBody SpeakMemberReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return imGroupMemberService.speak(req);
    }
}

package com.hao14293.im.service.friendship.controller;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.model.SyncReq;
import com.hao14293.im.service.friendship.model.req.*;
import com.hao14293.im.service.friendship.service.ImFriendShipService;
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
@Api(tags = "Friend")
@RestController
@RequestMapping("/v1/friendship")
public class ImFriendShipController {
    @Resource
    private ImFriendShipService imFriendShipService;

    @ApiOperation("导入关系链")
    @RequestMapping("/importFriendShip")
    public ResponseVO importFriendShip(@RequestBody ImportFriendShipReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.importFriendShip(req);
    }

    @ApiOperation("添加好友")
    @RequestMapping("/addFriendShip")
    public ResponseVO addFriendShip(@RequestBody AddFriendShipReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.addFriendShip(req);
    }

    @ApiOperation("修改好友关系")
    @RequestMapping("/updateFriendShip")
    public ResponseVO updateFriendShip(@RequestBody UpdateFriendReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.updateFriend(req);
    }

    @ApiOperation("删除好友关系")
    @RequestMapping("/deleteFriendShip")
    public ResponseVO deleteFriendShip(@RequestBody DeleteFriendReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.deleteFriend(req);
    }

    @ApiOperation("删除所有好友关系")
    @RequestMapping("/deleteAllFriendShip")
    public ResponseVO deleteAllFriendShip(@RequestBody DeleteFriendReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.deleteAllFriend(req);
    }

    @ApiOperation("拉取指定好友信息")
    @RequestMapping("/getRelation")
    public ResponseVO getRelation(@RequestBody GetRelationReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.getRelation(req);
    }

    @ApiOperation("拉取指定用户所有好友信息")
    @RequestMapping("/getAllFriendShip")
    public ResponseVO getAllFriendShip(@RequestBody GetAllFriendShipReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.getAllFriend(req);
    }

    @ApiOperation("校验好友关系")
    @RequestMapping("/checkFriendShip")
    public ResponseVO checkFriendShip(@RequestBody CheckFriendShipReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.checkFriendShip(req);
    }

    @ApiOperation("拉入黑名单")
    @RequestMapping("/addFriendSipBlack")
    public ResponseVO addFriendSipBlack(@RequestBody AddFriendShipBlackReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.addFriendShipBlack(req);
    }

    @ApiOperation("拉出黑名单")
    @RequestMapping("/deleteFriendSipBlack")
    public ResponseVO deleteFriendSipBlack(@RequestBody DeleteBlackReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.deleteFriendShipBlack(req);
    }

    @ApiOperation("校验黑名单")
    @RequestMapping("/checkFriendBlack")
    public ResponseVO checkFriendBlack(@RequestBody CheckFriendShipReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.checkFriendBlack(req);
    }

    @ApiOperation("同步好友列表")
    @RequestMapping("/syncFriendShipList")
    public ResponseVO syncFriendShipList(@RequestBody SyncReq req, Integer appId, String identifier){
        req.setAppId(appId);
        req.setOperater(identifier);
        return imFriendShipService.syncFriendShipList(req);
    }
}


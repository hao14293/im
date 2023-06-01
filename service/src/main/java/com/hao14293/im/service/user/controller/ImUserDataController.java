package com.hao14293.im.service.user.controller;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.user.model.req.GetUserInfoReq;
import com.hao14293.im.service.user.model.req.ModifyUserInfoReq;
import com.hao14293.im.service.user.service.ImUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Api(tags = "UserData")
@RestController
@RequestMapping("/v1/user/data")
public class ImUserDataController {

    @Resource
    private ImUserService imUserService;

    @ApiOperation(value = "批量获取用户信息")
    @PostMapping("/getBatchUserInfo")
    public ResponseVO getUsersInfo(@RequestBody GetUserInfoReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.getUserInfo(req);
    }

    @ApiOperation(value = "获取单个用户信息")
    @PostMapping("/getSingleUserInfo")
    public ResponseVO getSingleUserInfo(@Validated String userId, Integer appId) {
        return imUserService.getSingleUserInfo(userId, appId);
    }

    @ApiOperation(value = "修改用户信息")
    @PostMapping("/modifyUserInfo")
    public ResponseVO modifyUserInfo(@RequestBody ModifyUserInfoReq req, Integer appId){
        req.setAppId(appId);
        return imUserService.modifyUserInfo(req);
    }
}

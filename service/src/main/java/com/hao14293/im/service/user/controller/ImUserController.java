package com.hao14293.im.service.user.controller;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.user.model.req.DeleteUserReq;
import com.hao14293.im.service.user.model.req.ImportUserReq;
import com.hao14293.im.service.user.model.req.LoginReq;
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
@Api(tags = "Users")
@RestController
@RequestMapping("/v1/user")
public class ImUserController {

    @Resource
    private ImUserService imUserService;

    @ApiOperation(value = "导入用户")
    @PostMapping("/importUser")
    public ResponseVO importUser(@RequestBody ImportUserReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.importUser(req);
    }


    @ApiOperation(value = "删除用户的方法")
    @PostMapping("/deleteUser")
    public ResponseVO deleteUser(@RequestBody @Validated DeleteUserReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.deleteUser(req);
    }

    @ApiOperation(value = "登录")
    @PostMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq req, Integer appId) {
        req.setAppId(appId);
        ResponseVO login = imUserService.login(req);

        if (login.isOk()) {
            // TODO 去zk获取一个im的地址，返回给sdk

            return ResponseVO.successResponse();
        }
        return ResponseVO.errorResponse();
    }

}

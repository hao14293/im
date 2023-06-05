package com.hao14293.im.service.user.controller;

import com.hao14293.im.common.ClientType;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.route.RouteHandle;
import com.hao14293.im.common.route.RouteInfo;
import com.hao14293.im.common.utils.RouteInfoParseUtil;
import com.hao14293.im.service.user.model.req.*;
import com.hao14293.im.service.user.service.ImUserService;
import com.hao14293.im.service.user.service.ImUserStatusService;
import com.hao14293.im.service.utils.ZKit;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

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

    @Resource
    private RouteHandle routeHandle;

    @Resource
    private ImUserStatusService imUserStatusService;

    @Resource
    private ZKit zKit;

    @ApiOperation(value = "导入用户的方法")
    @RequestMapping("/importUser")
    public ResponseVO importUser(@RequestBody ImportUserReq req, Integer appId){
        req.setAppId(appId);
        return imUserService.importUser(req);
    }

    @ApiOperation(value = "删除用户的方法")
    @RequestMapping("/deleteUser")
    public ResponseVO deleteUser(@RequestBody @Validated DeleteUserReq req, Integer appId){
        req.setAppId(appId);
        return imUserService.deleteUser(req);
    }

    /**
     * im的接口，返回im的地址
     * @param req
     * @param appId
     * @return
     */
    @ApiOperation(value = "登录的方法")
    @RequestMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq req, Integer appId){
        req.setAppId(appId);

        ResponseVO login = imUserService.login(req);

        if(login.isOk()){
            // TODO 去zk获取一个im的地址，返回给sdk
            List<String> allNode;
            if(req.getClientType() == ClientType.WEB.getCode()){
                allNode = zKit.getAllWebNode();
            }else{
                allNode = zKit.getAllTcpNode();
            }
            // ip:port
            String s = routeHandle.routeServer(allNode, req.getUserId());
            RouteInfo parse = RouteInfoParseUtil.parse(s);
            return ResponseVO.successResponse(parse);
        }
        return ResponseVO.errorResponse();
    }

    @RequestMapping("/getUserSequence")
    public ResponseVO getUserSequence(@RequestBody
                                      GetUserSequenceReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.getUserSequence(req);
    }

    @RequestMapping("/subscribeUserOnlineStatus")
    public ResponseVO subscribeUserOnlineStatus(@RequestBody
                                                SubscribeUserOnlineStatusReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperater(identifier);
        imUserStatusService.subscribeUserOnlineStatus(req);
        return ResponseVO.successResponse();
    }

    @RequestMapping("/setUserCustomerStatus")
    public ResponseVO setUserCustomerStatus(@RequestBody
                                            SetUserCustomerStatusReq req, Integer appId,String identifier) {
        req.setAppId(appId);
        req.setOperater(identifier);
        imUserStatusService.setUserCustomerStatus(req);
        return ResponseVO.successResponse();
    }

    @RequestMapping("/queryFriendOnlineStatus")
    public ResponseVO queryFriendOnlineStatus(@RequestBody
                                              PullFriendOnlineStatusReq req, Integer appId,String identifier) {
        req.setAppId(appId);
        req.setOperater(identifier);
        return ResponseVO.successResponse(imUserStatusService.queryFriendOnlineStatus(req));
    }

    @RequestMapping("/queryUserOnlineStatus")
    public ResponseVO queryUserOnlineStatus(@RequestBody
                                            PullUserOnlineStatusReq req, Integer appId,String identifier) {
        req.setAppId(appId);
        req.setOperater(identifier);
        return ResponseVO.successResponse(imUserStatusService.queryUserOnlineStatus(req));
    }
}

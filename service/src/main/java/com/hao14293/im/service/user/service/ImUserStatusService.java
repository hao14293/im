package com.hao14293.im.service.user.service;

import com.hao14293.im.service.user.model.UserStatusChangeNotifyContent;
import com.hao14293.im.service.user.model.req.PullFriendOnlineStatusReq;
import com.hao14293.im.service.user.model.req.PullUserOnlineStatusReq;
import com.hao14293.im.service.user.model.req.SetUserCustomerStatusReq;
import com.hao14293.im.service.user.model.req.SubscribeUserOnlineStatusReq;
import com.hao14293.im.service.user.model.resp.UserOnlineStatusResp;

import java.util.Map;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
public interface ImUserStatusService {

    /**
     * 处理用户在线状态变更的通知
     */
    void processUserOnlineStatusNotify(UserStatusChangeNotifyContent content);

    /**
     * 订阅用户状态
     */
    void subscribeUserOnlineStatus(SubscribeUserOnlineStatusReq req);

    /**
     * 设置客户端状态
     */
    void setUserCustomerStatus(SetUserCustomerStatusReq req);

    /**
     * 拉取指定用户的状态
     */
    Map<String, UserOnlineStatusResp> queryUserOnlineStatus(PullUserOnlineStatusReq req);

    /**
     * 拉取所有用户的状态
     */
    Map<String, UserOnlineStatusResp> queryFriendOnlineStatus(PullFriendOnlineStatusReq req);
}

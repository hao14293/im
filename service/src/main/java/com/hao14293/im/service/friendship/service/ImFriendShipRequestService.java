package com.hao14293.im.service.friendship.service;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.friendship.model.req.ApproverFriendRequestReq;
import com.hao14293.im.service.friendship.model.req.FriendDto;
import com.hao14293.im.service.friendship.model.req.ReadFriendShipRequestReq;

/**
 * @Author: hao14293
 * @data 2023/4/13
 * @time 19:10
 */
public interface ImFriendShipRequestService {

    /**
     * 添加好友请求
     */
    ResponseVO addFriendshipRequest(String fromId, FriendDto dto, Integer appId);

    /**
     * 审批好友请求
     */
    ResponseVO approverFriendRequest(ApproverFriendRequestReq req);

    /**
     * 已读好友请求
     */
    public ResponseVO readFriendShipRequestReq(ReadFriendShipRequestReq req);

    /**
     * 获得好友请求
     */
    public ResponseVO getFriendRequest(String fromId, Integer appId);
}

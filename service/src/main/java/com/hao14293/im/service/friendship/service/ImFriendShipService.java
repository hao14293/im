package com.hao14293.im.service.friendship.service;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.model.RequestBase;
import com.hao14293.im.common.model.SyncReq;
import com.hao14293.im.service.friendship.model.req.*;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
public interface ImFriendShipService {

    /**
     * 导入关系链
     */
    ResponseVO importFriendShip(ImportFriendShipReq req);

    /**
     * 添加好友
     */
    ResponseVO addFriendShip(AddFriendShipReq req);

    /**
     * 添加好友具体逻辑
     */
    ResponseVO doAddFriend(RequestBase requestBase, String fromId, FriendDto dto, Integer appId);

    /**
     * 更新好友关系
     */
    ResponseVO updateFriend(UpdateFriendReq req);

    /**
     * 删除好友关系
     */
    ResponseVO deleteFriend(DeleteFriendReq req);

    /**
     * 删除所有好友
     */
    ResponseVO deleteAllFriend(DeleteFriendReq req);

    /**
     * 拉取指定好友信息
     */
    ResponseVO getRelation(GetRelationReq req);

    /**
     * 拉取所有好友信息
     */
    ResponseVO getAllFriend(GetAllFriendShipReq req);

    /**
     * 校验好友关系
     */
    ResponseVO checkFriendShip(CheckFriendShipReq req);

    /**
     * 加入黑名单
     */
    ResponseVO addFriendShipBlack(AddFriendShipBlackReq req);

    /**
     * 移除黑名单
     */
    ResponseVO deleteFriendShipBlack(DeleteBlackReq req);

    /**
     * 校验黑名单
     */
    ResponseVO checkFriendBlack(CheckFriendShipReq req);

    /**
     * 同步好友列表信息
     */
    ResponseVO syncFriendShipList(SyncReq req);

    /**
     * 获取指定用户的所有好友ID
     */
    List<String> getAllFriendId(String userId, Integer appId);
}

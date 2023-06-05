package com.hao14293.im.service.friendship.service;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.friendship.entity.ImFriendShipGroupEntity;
import com.hao14293.im.service.friendship.model.req.AddFriendShipGroupReq;
import com.hao14293.im.service.friendship.model.req.DeleteFriendShipGroupReq;

/**
 * @Author: hao14293
 * @data 2023/4/13
 * @time 21:03
 */
public interface ImFriendShipGroupService {

    /**
     * 添加分组
     */
    ResponseVO addGroup(AddFriendShipGroupReq req);

    /**
     * 删除分组
     */
    ResponseVO deleteGroup(DeleteFriendShipGroupReq req);

    /**
     * 获取分组
     */
    ResponseVO<ImFriendShipGroupEntity> getGroup(String fromId, String groupName, Integer appId);


    Long updateSeq(String fromId, String groupName, Integer appId);
}

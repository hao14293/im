package com.hao14293.im.service.friendship.service;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.hao14293.im.service.friendship.model.req.DeleteFriendShipGroupMemberReq;

/**
 * @Author: hao14293
 * @data 2023/4/13
 * @time 21:11
 */
public interface ImFriendShipGroupMemberService {

    /**
     * 添加组内成员
     */
    ResponseVO addGroupMember(AddFriendShipGroupMemberReq req);

    /**
     * 删除组内成员
     */
    ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req);

    /**
     * 添加组内成员具体逻辑
     */
    int doAddGroupMember(Long groupId, String toId);

    /**
     * 清空组内所有成员
     */
    int clearGroupMember(Long groupId);
}

package com.hao14293.im.service.group.service;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.model.SyncReq;
import com.hao14293.im.service.group.entity.ImGroupEntity;
import com.hao14293.im.service.group.model.req.*;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public interface ImGroupService {
    /**
     * 导入群
     */
    ResponseVO importGroup(ImportGroupReq req);

    /**
     * 新建群
     */
    ResponseVO createGroup(CreateGroupReq req);

    /**
     * 修改群信息
     */
    ResponseVO updateBaseGroupInfo(UpdateGroupReq req);

    /**
     * 获取群的具体信息
     */
    ResponseVO getGroupInfo(GetGroupInfoReq req);

    /**
     * 获取群信息
     */
    ResponseVO<ImGroupEntity> getGroup(String groupId, Integer appId);

    /**
     * 获取用户加入的群组
     */
    ResponseVO getJoinedGroup(GetJoinedGroupReq req);

    /**
     * 解散群组
     */
    ResponseVO destroyGroup(DestroyGroupReq req);

    /**
     * 转让群主
     */
    ResponseVO transferGroup(TransferGroupReq req);

    /**
     * 群组禁言
     */
    ResponseVO muteGroup(MuteGroupReq req);

    /**
     * 增量同步群组成员列表
     */
    ResponseVO syncJoinedGroupList(SyncReq req);

    /**
     * 动态获取群组中最大的seq
     */
    Long getUserGroupMaxSeq(String userId, Integer appId);
}

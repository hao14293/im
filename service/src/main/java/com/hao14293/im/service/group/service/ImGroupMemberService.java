package com.hao14293.im.service.group.service;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.group.model.req.*;
import com.hao14293.im.service.group.model.resp.GetRoleInGroupResp;

import java.util.Collection;
import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public interface ImGroupMemberService {

    /**
     *  导入群成员
     */
    ResponseVO importGroupMember(ImportGroupMemberReq req);

    /**
     * 新建群的时候要将群主加入群
     */
    ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDto dto);

    /**
     * 获取群组中某成员的role
     */
    ResponseVO<GetRoleInGroupResp> getRoleInGroupOne(String groupId, String memberId, Integer appId);

    /**
     * 获取群组中所有成员信息
     */
    ResponseVO<List<GroupMemberDto>> getGroupMember(String groupId, Integer appId);

    /**
     * 获取用户加入的所有群的 id
     */
    ResponseVO<Collection<String>> getMemberJoinedGroup(GetJoinedGroupReq req);

    /**
     * 转让群主
     */
    ResponseVO transferGroupMember(String owner, String groupId, Integer appId);

    /**
     * 拉人入群
     */
    ResponseVO addMember(AddGroupMemberReq req);

    /**
     * 踢人出群
     */
    ResponseVO removeMember(RemoveGroupMemberReq req);

    /**
     * 踢人出群 具体
     */
    ResponseVO removeGroupMember(String groupId, Integer appId, String memberId);

    /**
     * 退出群聊
     */
    ResponseVO exitGroup(ExitGroupReq req);

    /**
     * 修改群成员信息
     */
    ResponseVO updateGroupMember(UpdateGroupMemberReq req);

    /**
     * 禁言群成员
     */
    ResponseVO speak(SpeakMemberReq req);

    /**
     * 获取指定群组的所有成员id
     */
    List<String> getGroupMemberId(String groupId, Integer appId);

    /**
     * 获取群组中的管理员具体信息
     */
    List<GroupMemberDto> getGroupManager(String groupId, Integer appId);

    /**
     * 增量同步用户加入的群组
     */
    ResponseVO<Collection<String>> syncMemberJoinedGroup(String operater, Integer appId);
}

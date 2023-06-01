package com.hao14293.im.service.group.service;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.group.model.req.GroupMemberDto;
import com.hao14293.im.service.group.model.req.ImportGroupMemberReq;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public interface ImGroupMemberService {

    // 导入群成员
    ResponseVO importGroupMember(ImportGroupMemberReq req);

    // 新建群的时候要将群主加入群
    ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDto dto);
}

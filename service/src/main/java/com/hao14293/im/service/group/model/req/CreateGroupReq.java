package com.hao14293.im.service.group.model.req;

import com.hao14293.im.common.model.RequestBase;
import lombok.Data;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class CreateGroupReq extends RequestBase {

    private String groupId;

    private String ownerId;

    private Integer groupType;

    private String groupName;

    /**
     * 是否开启禁言
     * 0 不禁言 1 禁言
     */
    private Integer mute;

    /**
     * 加入群权限，0 所有人可以加入；1 群成员可以拉人；2 群管理员或群主可以拉人。
     */
    private Integer applyJoinType;

    //群简介
    private String introduction;

    //群公告
    private String notification;

    //群头像
    private String photo;

    private Integer MaxMemberCount;

    private List<GroupMemberDto> member;

    private String extra;
}

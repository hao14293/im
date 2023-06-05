package com.hao14293.im.codec.pack.group;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 * 修改群信息通知报文
 */
@Data
public class UpdateGroupInfoPack {

    private String groupId;

    private String groupName;

    // 是否全员禁言，0 不禁言；1 全员禁言。
    private Integer mute;

    //加入群权限，0 所有人可以加入；1 群成员可以拉人；2 群管理员或群组可以拉人。
    private Integer joinType;

    private String introduction;

    private String notification;

    private String photo;

    private Integer maxMemberCount;

    private Long sequence;
}

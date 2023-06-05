package com.hao14293.im.codec.pack.group;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 * 踢人出群通知报文
 */
@Data
public class RemoveGroupMemberPack {

    private String groupId;

    private String member;

}

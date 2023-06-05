package com.hao14293.im.codec.pack.group;

import lombok.Data;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 * 群内添加群成员通知报文
 */
@Data
public class AddGroupMemberPack {

    private String groupId;

    private List<String> members;

}

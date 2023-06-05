package com.hao14293.im.codec.pack.friendship;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 * 用户创建好友分组通知包
 */
@Data
public class AddFriendGroupPack {
    public String fromId;

    private String groupName;

    /** 序列号*/
    private Long sequence;
}

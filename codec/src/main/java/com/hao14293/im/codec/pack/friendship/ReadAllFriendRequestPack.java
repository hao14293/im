package com.hao14293.im.codec.pack.friendship;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 * 已读好友申请通知报文
 */
@Data
public class ReadAllFriendRequestPack {

    private String fromId;

    private Long sequence;
}


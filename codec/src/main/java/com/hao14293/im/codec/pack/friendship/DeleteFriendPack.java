package com.hao14293.im.codec.pack.friendship;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 * 删除好友通知报文
 */
@Data
public class DeleteFriendPack {

    private String fromId;

    private String toId;

    private Long sequence;
}
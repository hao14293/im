package com.hao14293.im.codec.pack.friendship;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 * 修改好友通知报文
 */
@Data
public class UpdateFriendPack {

    public String fromId;

    private String toId;

    private String remark;

    private Long sequence;
}

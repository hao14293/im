package com.hao14293.im.codec.pack.group;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 * 转让群主通知报文
 */
@Data
public class TransferGroupPack {

    private String groupId;

    private String ownerId;

}

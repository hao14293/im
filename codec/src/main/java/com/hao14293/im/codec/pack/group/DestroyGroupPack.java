package com.hao14293.im.codec.pack.group;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 * 解散群通知报文
 */

@Data
public class DestroyGroupPack {

    private String groupId;

    private Long sequence;

}
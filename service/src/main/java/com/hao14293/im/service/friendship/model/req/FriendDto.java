package com.hao14293.im.service.friendship.model.req;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
public class FriendDto {
    // 加谁好友
    private String toId;
    // 备注
    private String remark;
    // 添加来源
    private String addSource;
    // 其他
    private String extra;
    //
    private String addWorking;
}

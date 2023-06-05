package com.hao14293.im.service.friendship.model.callback;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
public class DeleteFriendAfterCallbackDto {

    private String fromId;

    private String toId;
}
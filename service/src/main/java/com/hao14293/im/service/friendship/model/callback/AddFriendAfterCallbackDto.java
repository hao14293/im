package com.hao14293.im.service.friendship.model.callback;

import com.hao14293.im.service.friendship.model.req.FriendDto;
import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
public class AddFriendAfterCallbackDto {
    private String fromId;
    private FriendDto toItem;
}

package com.hao14293.im.service.friendship.model.req;

import com.hao14293.im.common.enums.FriendShipStatusEnum;
import com.hao14293.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
public class ImportFriendShipReq extends RequestBase {

    @NotNull(message = "fromId不能为空")
    private String fromId;

    private List<ImportFriendDto> friendItem;
    @Data
    public static class ImportFriendDto{
        private String toId;

        private String remark;

        private String addSource;

        // 好友状态字段
        private Integer status = FriendShipStatusEnum.FRIEND_STATUS_NOT_FRIEND.getCode();
        // 拉黑状态
        private Integer black = FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode();
    }
}

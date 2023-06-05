package com.hao14293.im.service.friendship.model.req;

import com.hao14293.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
public class DeleteFriendShipGroupReq extends RequestBase {
    @NotBlank(message = "fromId不能为空")
    private String fromId;

    @NotEmpty(message = "分组名称不能为空")
    private List<String> groupName;

}
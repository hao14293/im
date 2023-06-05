package com.hao14293.im.service.friendship.model.req;

import com.hao14293.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
public class CheckFriendShipReq extends RequestBase {
    @NotBlank(message = "fromId不能为空")
    private String fromId;
    @NotEmpty(message = "toIds不能为空")
    private List<String> toIds;
    @NotNull(message = "参数校验不能为空")
    private Integer checkType;
}

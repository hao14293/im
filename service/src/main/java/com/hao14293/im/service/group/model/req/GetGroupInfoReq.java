package com.hao14293.im.service.group.model.req;

import com.hao14293.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class GetGroupInfoReq extends RequestBase {

    @NotBlank(message = "群id不能为空")
    private String groupId;
}

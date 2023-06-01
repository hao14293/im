package com.hao14293.im.service.group.model.req;

import com.hao14293.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class GetJoinedGroupReq extends RequestBase {

    @NotBlank(message = "用户id不能为空")
    private String memberId;

    // 群类型
    private List<Integer> groupType;

    /**
     * 单次拉取的群组数量，默认为所有群组
     */
    private Integer limit;

    /**
     * 第几页
     */
    private Integer offset;
}

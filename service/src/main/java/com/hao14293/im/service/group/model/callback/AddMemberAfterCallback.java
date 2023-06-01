package com.hao14293.im.service.group.model.callback;

import com.hao14293.im.service.group.model.resp.AddMemberResp;
import lombok.Data;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class AddMemberAfterCallback {

    private String groupId;

    private Integer groupType;

    private String operater;

    private List<AddMemberResp> memberId;
}

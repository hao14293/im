package com.hao14293.im.service.user.model.resp;

import com.hao14293.im.service.user.model.entity.ImUserDataEntity;
import lombok.Data;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class GetUserInfoResp {

    private List<ImUserDataEntity> userDataItem;
    private List<String> failUser;
}

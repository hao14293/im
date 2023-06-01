package com.hao14293.im.service.user.model;

import com.hao14293.im.common.model.ClientInfo;
import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class UserStatusChangeNotifyContent extends ClientInfo {

    private String userId;

    /**
     * 1 上线 2 离线
     */
    private Integer status;
}

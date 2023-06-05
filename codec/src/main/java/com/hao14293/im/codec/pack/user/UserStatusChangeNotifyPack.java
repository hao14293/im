package com.hao14293.im.codec.pack.user;

import com.hao14293.im.common.model.UserSession;
import lombok.Data;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
public class UserStatusChangeNotifyPack {

    private Integer appId;

    private String userId;

    private Integer status;

    private List<UserSession> client;
}


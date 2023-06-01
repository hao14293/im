package com.hao14293.im.service.user.model.resp;

import com.hao14293.im.common.model.UserSession;
import lombok.Data;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class UserOnlineStatusResp {

    private List<UserSession> sessions;

    private String customText;

    private Integer customStatus;
}

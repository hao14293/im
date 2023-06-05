package com.hao14293.im.service.friendship.model.resp;

import lombok.Data;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
public class ImportFriendShipResp {
    private List<String> successId;
    private List<String> errorId;
}
